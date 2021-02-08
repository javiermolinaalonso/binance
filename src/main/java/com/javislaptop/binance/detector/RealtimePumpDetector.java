package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.OrderBook;
import com.javislaptop.binance.api.Binance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(PumpDetectorProperties.class)
public class RealtimePumpDetector {

    private static final long MILLIS_TO_GROUP = 100;
    private final BinanceApiWebSocketClient binanceWebsocket;
    private final PumpDetectorProperties pumpDetectorProperties;
    private final Binance binance;

    private final ConcurrentHashMap<String, List<AggTradeEvent>> eventsMap;
    private AtomicInteger counter = new AtomicInteger(0);

    public RealtimePumpDetector(BinanceApiWebSocketClient binanceWebsocket, PumpDetectorProperties pumpDetectorProperties, Binance binance) {
        this.binanceWebsocket = binanceWebsocket;
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.binance = binance;
        this.eventsMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        BinanceApiCallback<AggTradeEvent> binanceApiCallback = new BinanceApiCallback<>() {

            @Override
            public void onResponse(AggTradeEvent response) {
                eventsMap.get(response.getSymbol()).add(response);
            }

            @Override
            public void onFailure(Throwable cause) {
                System.out.println(cause.getMessage());
            }
        };
        pumpDetectorProperties
                .getSymbols()
                .forEach(
                        t -> {
                            eventsMap.put(t, new ArrayList<>());
                            binanceWebsocket.onAggTradeEvent(t.toLowerCase(), binanceApiCallback);
                        }
                );
    }

    public void showPumps() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runPump();
            }
        }, 0, 100);
    }

    public void runPump() {
        try {
            eventsMap.entrySet()
                    .stream().filter(k -> !k.getValue().isEmpty())
                    .forEach(k -> runPump(k.getKey()));
            if (counter.incrementAndGet() % 100 == 0) {
                System.out.println();
            }
            System.out.print(".");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void runPump(String symbol) {
        List<AggTradeEvent> trades = new ArrayList<>(eventsMap.get(symbol));
        eventsMap.get(symbol).clear();

        Map<Instant, List<AggTradeEvent>> occurrences = trades
                .stream()
                .collect(
                        Collectors.groupingBy(
                                trade -> Instant.ofEpochMilli(trade.getTradeTime() - (trade.getTradeTime() % MILLIS_TO_GROUP))
                        ));

        Optional<PumpData> pumpData = occurrences.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(t -> new PumpData(t.getKey(), t.getValue()))
                .filter(this::isPumpDetected)
                .findFirst();

        if (pumpData.isPresent()) {
            PumpData p = pumpData.get();
            System.out.println();
            System.out.println(String.format("PUMP DETECTED!!!! %s: Symbol: %s, Volume: %.5f, Ratio: %.5f, Trades: %s. Buys: %d, Sell: %d, Unknown: %d. Initial Price: %.8f. Final price: %.8f", symbol, p.getWhen(), p.getVolume(), p.getMakerRatio(), p.getTrades(), p.getBuys(), p.getSells(), p.getBuyOrSell(), p.getInitialPrice(), p.getFinalPrice()));
            OrderBook orderBook = binance.getOrderBook(symbol);
            double averageAsk = orderBook.getAsks().stream().mapToDouble(ask -> PumpData.extractVolume(ask.getPrice(), ask.getQty())).sum() / orderBook.getAsks().stream().mapToDouble(k -> new BigDecimal(k.getQty()).doubleValue()).sum();
            double price = p.getFinalPrice().doubleValue();
            double askDistance = (averageAsk / price) - 1;
            if (askDistance > pumpDetectorProperties.getAverageAskDistance()) {
                System.out.println(String.format("PUMP CONFIRMED!!!! Ask distance is %.4f", askDistance));
            } else {
                System.out.println(String.format("PUMP DISCARDED. Ask distance is %.4f", askDistance));
            }

            List<Candlestick> lastHour = binance.getLastHour(symbol);
            double averageVolumePer100millis = lastHour.stream().mapToDouble(c -> new BigDecimal(c.getVolume()).doubleValue()).average().orElse(0d) / 600d;
            double volumeIncrease = (p.getVolume() / averageVolumePer100millis) - 1;
            if (volumeIncrease > pumpDetectorProperties.getVolumeRatio()) {
                System.out.println(String.format("PUMP CONFIRMED!!!! Volume increase is %.4f", volumeIncrease));
            } else {
                System.out.println(String.format("PUMP DISCARDED. Volume increase is %.4f", volumeIncrease));
            }

        }
    }

    private boolean isPumpDetected(PumpData pData) {
        return pData.getTrades() > pumpDetectorProperties.getMinTrades()
                && pData.getBuys() > pumpDetectorProperties.getMinBuys()
                && pData.getPriceIncrease() > pumpDetectorProperties.getMinIncrease()
                && pData.getMakerRatio() < pumpDetectorProperties.getMaxBuyerRatio();
    }
}

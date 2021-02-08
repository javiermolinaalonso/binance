package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.AggTrade;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class RealtimeRestDetector {

    private static final int MILLIS_TO_GROUP = 100; // 60000 = 1 minute, 1000 = 1 second

    private final BinanceApiRestClient binance;
    private List<String> btcPairs;

    public RealtimeRestDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    @PostConstruct
    public void init() {
        btcPairs = binance.getExchangeInfo().getSymbols().stream()
                .filter(symbol -> symbol.getSymbol().endsWith("BTC"))
                .map(SymbolInfo::getSymbol)
                .collect(toList());
    }

    public void showPumps() {
        btcPairs.forEach(pair -> {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runPump(pair);
                }
            }, 0, 60000);
        });
    }

    public void runPump(String symbol) {
        Instant to = Instant.now(Clock.systemUTC()).plus(1, ChronoUnit.SECONDS);
        Instant from = Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.SECONDS);

        List<AggTrade> trades = binance.getAggTrades(symbol, null, 100000, from.toEpochMilli(), to.toEpochMilli());

        Map<Instant, List<AggTrade>> occurrences = trades
                .stream()
                .collect(
                        Collectors.groupingBy(
                                trade -> Instant.ofEpochMilli(trade.getTradeTime() - (trade.getTradeTime() % MILLIS_TO_GROUP))
                        ));

        Optional<PumpData> pumpData = occurrences.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(t -> new PumpData(t.getKey(), t.getValue()))
                .filter(t -> t.getTrades() > 10)
                .filter(t -> t.getBuys() > 5)
                .filter(t -> t.getPriceIncrease() > 1)
                .filter(t -> t.getMakerRatio() < 0.1)
                .findFirst();
//        pumpData.ifPresentOrElse(
//                e -> {
//                    System.out.println();
//                    System.out.println(String.format("PUMP DETECTED!!!! %s: Symbol: %s, Volume: %.5f, Ratio: %.5f, Trades: %s. Buys: %d, Sell: %d, Unknown: %d. Initial Price: %.8f. Final price: %.8f", symbol, e.getWhen(), e.getVolume(), e.getMakerRatio(), e.getTrades(), e.getBuys(), e.getSells(), e.getBuyOrSell(), e.getInitialPrice(), e.getFinalPrice()));
//                },
//                () -> System.out.print(".")
//        );
        pumpData.ifPresent(
                e -> System.out.println(String.format("PUMP DETECTED!!!! %s: Symbol: %s, Volume: %.5f, Ratio: %.5f, Trades: %s. Buys: %d, Sell: %d, Unknown: %d. Initial Price: %.8f. Final price: %.8f", symbol, e.getWhen(), e.getVolume(), e.getMakerRatio(), e.getTrades(), e.getBuys(), e.getSells(), e.getBuyOrSell(), e.getInitialPrice(), e.getFinalPrice()))
        );
    }
}

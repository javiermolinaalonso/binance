package com.javislaptop.binance.detector;

import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.OrderBook;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PumpInstantDetector {

    private final PumpDetectorProperties pumpDetectorProperties;
    private final Binance binance;

    public PumpInstantDetector(PumpDetectorProperties pumpDetectorProperties, Binance binance) {
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.binance = binance;
    }

    public <T extends AggTrade> boolean detect(String symbol, List<T> trades) {
        Map<Instant, List<T>> occurrences = trades
                .stream()
                .sorted(Comparator.comparingLong(AggTrade::getTradeTime))
                .collect(
                        Collectors.groupingBy(
                                trade -> Instant.ofEpochMilli(trade.getTradeTime() - (trade.getTradeTime() % pumpDetectorProperties.getTimeToDetect()))
                        ));

        List<PumpData> pumpData = occurrences.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(t -> new PumpData(t.getKey(), t.getValue()))
                .collect(Collectors.toList());

        pumpData
                .stream()
                .filter(this::isPumpDetected)
                .forEach(p -> {
            System.out.println(String.format("PUMP DETECTED!!!! %s %s", symbol, p.toString()));

            if (pumpDetectorProperties.isOrderBookEnabled()) {
                OrderBook orderBook = binance.getOrderBook(symbol);
                double averageAsk = orderBook.getAsks().stream().mapToDouble(ask -> PumpData.extractVolume(ask.getPrice(), ask.getQty())).sum() / orderBook.getAsks().stream().mapToDouble(k -> new BigDecimal(k.getQty()).doubleValue()).sum();
                double price = p.getFinalPrice().doubleValue();
                double askDistance = (averageAsk / price) - 1;
                if (askDistance > pumpDetectorProperties.getAverageAskDistance()) {
                    System.out.println(String.format("PUMP CONFIRMED!!!! Ask distance is %.4f", askDistance));
                } else {
                    System.out.println(String.format("PUMP DISCARDED. Ask distance is %.4f", askDistance));
                }
            }

            if (pumpDetectorProperties.isVolumeEnabled()) {
                double hourlyAverage = getAverage(binance.getLastHour(symbol, p.getWhen()));
                double lastMinuteAverage = getAverage(binance.getLastMinute(symbol, p.getWhen()));
                double pumpAverage = p.getVolume();

                double volumeIncrease = (pumpAverage / hourlyAverage) - 1;
                double volumeIncreaseLastMinute = (lastMinuteAverage / hourlyAverage) - 1;
                System.out.println(String.format("Volume increase last minute: %.4f. Volume last minute: %.4f. Volume last hour: %.4f", volumeIncreaseLastMinute, lastMinuteAverage, hourlyAverage));
                if (volumeIncrease > pumpDetectorProperties.getVolumeRatio()) {
                    System.out.println(String.format("PUMP CONFIRMED!!!! Volume increase is %.4f", volumeIncrease));
                } else {
                    System.out.println(String.format("PUMP DISCARDED. Volume increase is %.4f", volumeIncrease));
                }
            }

        });
        return false;
    }

    private double getAverage(List<Candlestick> values) {
        double lastHourAverageVolumePerMinute = values.stream().mapToDouble(c -> new BigDecimal(c.getVolume()).doubleValue()).average().orElse(0d);
        double lastHoursAveragePerSecond = lastHourAverageVolumePerMinute/60;
        return lastHoursAveragePerSecond * pumpDetectorProperties.getTimeToDetect() / 1000;
    }

    private boolean isPumpDetected(PumpData pData) {
        return pData.getTrades() > pumpDetectorProperties.getMinTrades()
                && pData.getBuys() > pumpDetectorProperties.getMinBuys()
                && pData.getPriceIncrease() > pumpDetectorProperties.getMinIncrease()
                && pData.getMakerRatio() < pumpDetectorProperties.getMaxBuyerRatio();
    }
}

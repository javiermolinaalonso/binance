package com.javislaptop.binance.detector;

import com.binance.api.client.domain.market.AggTrade;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.strategy.PurchaseAndSellQuickStrategy;
import com.javislaptop.binance.strategy.TradeStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class PumpInstantDetector {

    private final PumpDetectorProperties pumpDetectorProperties;
    private final TradeStrategy strategy;

    public PumpInstantDetector(PumpDetectorProperties pumpDetectorProperties, TradeStrategy strategy) {
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.strategy = strategy;
    }

    public <T extends AggTrade> void detect(String symbol, List<T> trades, Consumer<String> callback) {
        trades
                .stream()
                .sorted(Comparator.comparingLong(AggTrade::getTradeTime))
                .collect(
                        Collectors.groupingBy(
                                trade -> Instant.ofEpochMilli(trade.getTradeTime() - (trade.getTradeTime() % pumpDetectorProperties.getTimeToDetect()))
                        ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(t -> new PumpData(t.getKey(), t.getValue()))
                .filter(this::isPumpDetected)
                .findAny()
                .ifPresent(p -> {
                    System.out.println(String.format("PUMP DETECTED!!!! %s %s", symbol, p.toString()));
                    callback.accept(symbol);
                    strategy.execute(symbol);
                });
    }

    private boolean isPumpDetected(PumpData pData) {
        return pData.getTrades() > pumpDetectorProperties.getMinTrades()
                && pData.getBuys() > pumpDetectorProperties.getMinBuys()
                && pData.getMakerRatio() < pumpDetectorProperties.getMaxBuyerRatio();
    }
}

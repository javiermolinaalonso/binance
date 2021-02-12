package com.javislaptop.binance.detector.pump;

import com.binance.api.client.domain.market.AggTrade;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import com.javislaptop.binance.strategy.TradeStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PumpInstantDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);

    private final PumpDetectorProperties pumpDetectorProperties;
    private final TradeStrategy strategy;
    private final StreamDataStorage storage;

    public PumpInstantDetector(PumpDetectorProperties pumpDetectorProperties, TradeStrategy strategy, StreamDataStorage storage) {
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.strategy = strategy;
        this.storage = storage;
    }

    public <T extends AggTrade> void detect(String symbol) {
        detect(symbol, storage.read(symbol));
    }

    public <T extends AggTrade> void detect(String symbol, List<T> trades) {
        trades.stream()
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
                    logger.info("PUMP DETECTED!!!! {} {}", symbol, p.toString());
                    strategy.execute(symbol);
                });
    }

    private boolean isPumpDetected(PumpData pData) {
        return pData.getTrades() > pumpDetectorProperties.getMinTrades()
                && pData.getBuys() > pumpDetectorProperties.getMinBuys()
                && pData.getMakerRatio() < pumpDetectorProperties.getMaxBuyerRatio();
    }
}

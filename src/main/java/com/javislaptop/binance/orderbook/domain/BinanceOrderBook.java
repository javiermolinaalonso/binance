package com.javislaptop.binance.orderbook.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class BinanceOrderBook {

    private final List<BinanceOrderBookEntry> bids;
    private final List<BinanceOrderBookEntry> asks;
    private final Instant instant;
    private final Long lastUpdateId;

    public BinanceOrderBook(List<BinanceOrderBookEntry> bids, List<BinanceOrderBookEntry> asks, Instant instant, Long lastUpdateId) {
        this.bids = bids;
        this.asks = asks;
        this.instant = instant;
        this.lastUpdateId = lastUpdateId;
    }

    public List<BinanceOrderBookEntry> getBids() {
        return bids;
    }

    public List<BinanceOrderBookEntry> getAsks() {
        return asks;
    }

    public Instant getInstant() {
        return instant;
    }

    public Long getLastUpdateId() {
        return lastUpdateId;
    }

    public BigDecimal getGap() {
        return getAsk().subtract(getBid()).divide(getBid(), 4, RoundingMode.HALF_DOWN);
    }

    public Optional<BinanceOrderBookEntry> getFloor(BigDecimal threshold) {
        return findStepGreaterThan(bids, threshold);
    }

    public Optional<BinanceOrderBookEntry> getResistance(BigDecimal threshold) {
        return findStepGreaterThan(asks, threshold);
    }

    public Optional<BigDecimal> getDistanceBetweenFloorAndResistance(BigDecimal threshold) {
        Optional<BinanceOrderBookEntry> floor = getFloor(threshold);
        Optional<BinanceOrderBookEntry> resistance = getResistance(threshold);
        if (floor.isEmpty() || resistance.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resistance.get().getPrice().subtract(floor.get().getPrice()).divide(resistance.get().getPrice(), 4, RoundingMode.DOWN));
    }

    private Optional<BinanceOrderBookEntry> findStepGreaterThan(List<BinanceOrderBookEntry> values, BigDecimal threshold) {
        BigDecimal total = BigDecimal.valueOf(values.stream().mapToDouble(entry -> entry.getVolume().doubleValue()).sum());
        return values
                .stream()
                .filter(ask -> ask.getVolume().divide(total, 4, RoundingMode.DOWN).compareTo(threshold) > 0)
                .findFirst();
    }

    public BigDecimal getBid() {
        return bids.get(0).getPrice();
    }

    public BigDecimal getAsk() {
        return asks.get(0).getPrice();
    }
}

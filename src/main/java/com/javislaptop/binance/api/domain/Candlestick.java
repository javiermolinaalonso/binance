package com.javislaptop.binance.api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class Candlestick {
    private final Instant openTime;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal volume;
    private final Instant closeTime;
    private final BigDecimal quoteAssetVolume;
    private final Long numberOfTrades;
    private final BigDecimal takerBuyBaseAssetVolume;
    private final BigDecimal takerBuyQuoteAssetVolume;

    public Candlestick(Instant openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, Instant closeTime, BigDecimal quoteAssetVolume, Long numberOfTrades, BigDecimal takerBuyBaseAssetVolume, BigDecimal takerBuyQuoteAssetVolume) {
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.closeTime = closeTime;
        this.quoteAssetVolume = quoteAssetVolume;
        this.numberOfTrades = numberOfTrades;
        this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
        this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
    }

    public Candlestick(Long openTime, String open, String high, String low, String close, String volume, Long closeTime, String quoteAssetVolume, Long numberOfTrades, String takerBuyBaseAssetVolume, String takerBuyQuoteAssetVolume) {
        this(Instant.ofEpochMilli(openTime),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                new BigDecimal(volume),
                Instant.ofEpochMilli(closeTime),
                new BigDecimal(quoteAssetVolume),
                numberOfTrades,
                new BigDecimal(takerBuyBaseAssetVolume),
                new BigDecimal(takerBuyQuoteAssetVolume)
        );
    }

    public Instant getOpenTime() {
        return openTime;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public BigDecimal getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public Long getNumberOfTrades() {
        return numberOfTrades;
    }

    public BigDecimal getTakerBuyBaseAssetVolume() {
        return takerBuyBaseAssetVolume;
    }

    public BigDecimal getTakerBuyQuoteAssetVolume() {
        return takerBuyQuoteAssetVolume;
    }

    public BigDecimal getAmplitude() {
        return getHigh().divide(getLow(), RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE);
    }

    public BigDecimal getLowerShadow() {
        return getClose().divide(getLow(), RoundingMode.HALF_DOWN).min(getOpen().divide(getLow(), RoundingMode.HALF_DOWN)).subtract(BigDecimal.ONE);
    }

    public boolean isBullish() {
        return getClose().compareTo(getOpen()) > 0;
    }
}

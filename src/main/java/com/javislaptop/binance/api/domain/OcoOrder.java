package com.javislaptop.binance.api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OcoOrder {
    private final BigDecimal buyPrice;
    private final BigDecimal profitPrice;
    private final BigDecimal lossPrice;

    public OcoOrder(BigDecimal buyPrice, BigDecimal profitPrice, BigDecimal lossPrice) {
        this.buyPrice = buyPrice;
        this.profitPrice = profitPrice;
        this.lossPrice = lossPrice;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public BigDecimal getProfitPrice() {
        return profitPrice;
    }

    public BigDecimal getLossPrice() {
        return lossPrice;
    }

    public BigDecimal getProfitPercent() {
        return profitPrice.subtract(buyPrice).multiply(new BigDecimal(100)).divide(buyPrice, 4, RoundingMode.HALF_DOWN);
    }

    public BigDecimal getLossPercent() {
        return lossPrice.subtract(buyPrice).multiply(new BigDecimal(100)).divide(buyPrice, 4, RoundingMode.HALF_DOWN);
    }
}

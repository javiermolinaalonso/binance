package com.javislaptop.binance.orderbook.domain;

import java.math.BigDecimal;

public class BinanceOrderBookEntry {
    private final BigDecimal price;
    private final BigDecimal quantity;

    public BinanceOrderBookEntry(BigDecimal price, BigDecimal quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getVolume() {
        return price.multiply(quantity);
    }
}

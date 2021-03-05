package com.javislaptop.binance.detector.martingala;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class Trade {
    private final Instant when;
    private final BigDecimal amount;
    private final BigDecimal price;
    private final String symbol;
    private final String direction;

    public Trade(Instant when, BigDecimal amount, BigDecimal price, String symbol, String direction) {
        this.when = when;
        this.amount = amount;
        this.price = price;
        this.symbol = symbol;
        this.direction = direction;
    }

    public Instant getWhen() {
        return when;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s %s at %s", when.truncatedTo(ChronoUnit.MINUTES), direction, amount, symbol, price);
    }
}

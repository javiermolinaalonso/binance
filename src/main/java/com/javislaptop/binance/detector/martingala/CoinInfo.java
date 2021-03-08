package com.javislaptop.binance.detector.martingala;

public class CoinInfo {
    private final String symbol;
    private final Integer precision;

    public CoinInfo(String symbol, Integer precision) {
        this.symbol = symbol;
        this.precision = precision;
    }

    public String getSymbol() {
        return symbol;
    }

    public Integer getPrecision() {
        return precision;
    }
}

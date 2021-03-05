package com.javislaptop.binance.api;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.general.SymbolInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class BinanceFormatter {

    private final Map<Integer, DecimalFormat> formatters;
    private final BinanceApiRestClient binanceApiRestClient;
    private ExchangeInfo exchangeInfo;

    public BinanceFormatter(BinanceApiRestClient binanceApiRestClient) {
        this.binanceApiRestClient = binanceApiRestClient;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
        otherSymbols.setDecimalSeparator('.');
        formatters = Map.of(
                0, new DecimalFormat("0", otherSymbols),
                1, new DecimalFormat("0.0", otherSymbols),
                2, new DecimalFormat("0.00", otherSymbols),
                3, new DecimalFormat("0.000", otherSymbols),
                4, new DecimalFormat("0.0000", otherSymbols),
                5, new DecimalFormat("0.00000", otherSymbols),
                6, new DecimalFormat("0.000000", otherSymbols),
                7, new DecimalFormat("0.0000000", otherSymbols),
                8, new DecimalFormat("0.00000000", otherSymbols)
        );
    }

    @PostConstruct
    public void initialize() {
        this.exchangeInfo = binanceApiRestClient.getExchangeInfo();
    }

    public Integer getTradeDecimals(String symbol) {
        return getDecimals(symbol, FilterType.PRICE_FILTER, SymbolFilter::getTickSize);
    }

    public Integer getBaseAssetPrecision(String symbol) {
        return exchangeInfo.getSymbolInfo(symbol).getBaseAssetPrecision();
    }

    public Integer getQuotePrecision(String symbol) {
        return exchangeInfo.getSymbolInfo(symbol).getQuotePrecision();
    }

    public String formatPrice(String symbol, BigDecimal value) {
        return getPriceFormatter(symbol).format(value);
    }

    public String formatAmount(String symbol, BigDecimal amount) {
        return getAmountFormatter(symbol).format(amount);
    }

    private DecimalFormat getAmountFormatter(String symbol) {
        return formatters.get(getDecimals(symbol, FilterType.LOT_SIZE, SymbolFilter::getStepSize));
    }

    private DecimalFormat getPriceFormatter(String symbol) {
        return formatters.get(getDecimals(symbol, FilterType.PRICE_FILTER, SymbolFilter::getTickSize));
    }

    private int getDecimals(String symbol, FilterType filterType, Function<SymbolFilter, String> function) {
        String tickSize = getSymbolInfo(symbol).getFilters().stream().filter(f -> f.getFilterType() == filterType).findAny().map(function).orElse("0.00000001");

        switch (tickSize) {
            case "1.00000000": return 0;
            case "0.10000000": return 1;
            case "0.01000000": return 2;
            case "0.00100000": return 3;
            case "0.00010000": return 4;
            case "0.00001000": return 5;
            case "0.00000100": return 6;
            case "0.00000010": return 7;
            default: return 8;

        }
    }

    private SymbolInfo getSymbolInfo(String symbol) {
        return exchangeInfo.getSymbols().parallelStream().filter(x -> x.getSymbol().equals(symbol)).findAny().orElseThrow(()-> new RuntimeException("Pair not available"));
    }
}

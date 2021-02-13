package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderBookTrader {

    private static final BigDecimal RESISTANCE_THRESHOLD = new BigDecimal("0.08");
    private static final BigDecimal MIN_GAP = new BigDecimal("0.005");
    private static final BigDecimal MAX_GAP = new BigDecimal("0.1");

    private final Binance binance;
    private final OrderBookTraderWatcher watcher;


    public OrderBookTrader(Binance binance, OrderBookTraderWatcher watcher) {
        this.binance = binance;
        this.watcher = watcher;
    }

    public void execute() {
        List<SymbolInfo> collect = binance.getExchangeInfo().getSymbols().stream()
                .filter(s -> s.getStatus() == SymbolStatus.TRADING)
                .filter(s -> s.getSymbol().endsWith("BTC")).collect(Collectors.toList());

        for (SymbolInfo symbol : collect) {
            watcher.watch(symbol);
        }

    }


}

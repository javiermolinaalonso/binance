package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;

@Service
public class OrderBookTrader {

    private final Binance binance;
    private final OrderBookTraderWatcher watcher;


    public OrderBookTrader(Binance binance, OrderBookTraderWatcher watcher) {
        this.binance = binance;
        this.watcher = watcher;
    }

    public void execute() {
        binance.getExchangeInfo().getSymbols().stream()
                .filter(s -> s.getStatus() == SymbolStatus.TRADING)
                .filter(s -> s.getSymbol().endsWith("BTC"))
                .map(SymbolInfo::getSymbol)
                .forEach(this::execute);
    }

    public void execute(String symbol) {
        watcher.watch(symbol);
    }


}

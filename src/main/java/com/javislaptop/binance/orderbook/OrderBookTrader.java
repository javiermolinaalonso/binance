package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import com.javislaptop.binance.api.AccountService;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class OrderBookTrader {

    private static final List<String> symbols = List.of("COSBTC", "CELRBTC", "GOBTC");

    private final Binance binance;
    private final AccountService accountService;

    public OrderBookTrader(Binance binance, AccountService accountService) {
        this.binance = binance;
        this.accountService = accountService;
    }

    public void execute() {
        binance.getExchangeInfo().getSymbols().stream()
                .filter(s -> s.getStatus() == SymbolStatus.TRADING)
                .filter(s -> s.getSymbol().endsWith("BTC"))
                .filter(s -> symbols.contains(s.getSymbol()))
                .forEach(this::execute);
    }

    public void execute(SymbolInfo symbolInfo) {
        TimerTask placeOrderTask = new MonitorOrderBookTask(accountService, symbolInfo, binance);
        new Timer("monitor-orderbook-"+symbolInfo.getSymbol()).schedule(placeOrderTask, 1000, 15000);
    }


}

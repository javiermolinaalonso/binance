package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.OrderBook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PumpDetector {

    private final BinanceApiRestClient binance;

    public PumpDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    public void showPumps() {
//        List<String> btcPairs = binance.getExchangeInfo().getSymbols().stream()
//                .filter(symbol -> symbol.getSymbol().endsWith("BTC"))
//                .map(SymbolInfo::getSymbol)
//                .collect(Collectors.toList());
        binance.getCandlestickBars("BNBBTC", CandlestickInterval.ONE_MINUTE)
                .forEach(c -> {
                    System.out.println(c);
                });

    }
}

package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.pumper.UnusualVolumeDetector;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PumpDetector {

    private final BinanceApiRestClient binance;
    private final UnusualVolumeDetector volumeDetector;
    private final AskOutOfMoneyDetector askOutOfMoneyDetector;

    public PumpDetector(BinanceApiRestClient binance, UnusualVolumeDetector volumeDetector, AskOutOfMoneyDetector askOutOfMoneyDetector) {
        this.binance = binance;
        this.volumeDetector = volumeDetector;
        this.askOutOfMoneyDetector = askOutOfMoneyDetector;
    }

    public void showPumps() {
        List<String> btcPairs = binance.getExchangeInfo().getSymbols().stream()
                .filter(symbol -> symbol.getSymbol().endsWith("BTC"))
                .map(SymbolInfo::getSymbol)
                .collect(Collectors.toList());

        btcPairs.forEach(pair -> {
            boolean orderBookOverAskOutOfMoney = askOutOfMoneyDetector.isOrderBookOverAskOutOfMoney(pair, 50);
            if (orderBookOverAskOutOfMoney) {
                System.out.println(pair);
            }
        });

    }
}

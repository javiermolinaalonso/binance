package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.javislaptop.binance.pumper.UnusualVolumeDetector;
import org.springframework.stereotype.Service;

@Service
public class PumpDetector {

    private final BinanceApiRestClient binance;
    private final UnusualVolumeDetector volumeDetector;

    public PumpDetector(BinanceApiRestClient binance, UnusualVolumeDetector volumeDetector) {
        this.binance = binance;
        this.volumeDetector = volumeDetector;
    }

    public void showPumps() {
//        List<String> btcPairs = binance.getExchangeInfo().getSymbols().stream()
//                .filter(symbol -> symbol.getSymbol().endsWith("BTC"))
//                .map(SymbolInfo::getSymbol)
//                .collect(Collectors.toList());
        volumeDetector.detect();

    }
}

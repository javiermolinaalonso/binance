package com.javislaptop.binance.pumper;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import org.springframework.stereotype.Service;

@Service
public class UnusualVolumeDetector {

    private final BinanceApiRestClient binance;

    public UnusualVolumeDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    public void detect() {
        binance.getCandlestickBars("BNBBTC", CandlestickInterval.ONE_MINUTE);
    }
}

package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.BinanceFormatter;
import com.javislaptop.binance.api.domain.Candlestick;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(MartingalaDetector.class);

    final MartingalaProperties props;
    final WalletState wallet;

    MartingalaDetector(MartingalaProperties martingalaProperties, WalletState wallet) {
        this.props = martingalaProperties;
        this.wallet = wallet;
    }

    abstract Map<String, List<Candlestick>> populate();

    abstract BigDecimal updateBuyThreshold(BigDecimal referencePrice, String symbol, int attempts);

    public void execute() {
        Map<String, List<Candlestick>> data = populate();

        int limit = data.values().stream().map(List::size).findAny().get();
        for (int i = 1; i < limit; i++) {
            for (String symbol : data.keySet()) {
                List<Candlestick> candlesticks = data.get(symbol);
                Candlestick c = candlesticks.get(i);
                int purchases = 1;
                BigDecimal buyThreshold = updateBuyThreshold(c.getOpen(), symbol, purchases);
                while (c.getLow().compareTo(buyThreshold) < 0) {
                    wallet.buy(symbol, props.getBaseAmount(), buyThreshold, c.getCloseTime());
                    buyThreshold = updateBuyThreshold(buyThreshold, symbol, ++purchases);
                }
            }
            for (String symbol : data.keySet()) {
                Candlestick c = data.get(symbol).get(i);
                wallet.sell(symbol, c.getClose(), c.getCloseTime());
            }
        }
        wallet.printInfo();
    }


}

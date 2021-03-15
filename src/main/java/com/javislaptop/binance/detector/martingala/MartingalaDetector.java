package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.BinanceFormatter;
import com.javislaptop.binance.api.domain.Candlestick;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(MartingalaDetector.class);

    private final MartingalaLoader loader;
    private final BinanceFormatter binanceFormatter;
    private final MartingalaProperties props;
    private final WalletState wallet;

    public MartingalaDetector(MartingalaLoader loader, BinanceFormatter binanceFormatter, MartingalaProperties martingalaProperties, WalletState wallet) {
        this.loader = loader;
        this.binanceFormatter = binanceFormatter;
        this.props = martingalaProperties;
        this.wallet = wallet;
    }

    public void execute() {
        Map<String, List<Candlestick>> data = loader.load(props.getFrom(), props.getTo(), props.getTradingCurrency(), props.getBaseCurrency(), props.getInterval());

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

    private BigDecimal updateBuyThreshold(BigDecimal referencePrice, String symbol, int attempts) {
        Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);
        BigDecimal ratio = props.getDecrease();
        if (props.isAutomaticThreshold()) {
            ratio = ratio.multiply(new BigDecimal(attempts));
        }
        return referencePrice.subtract(referencePrice.multiply(ratio)).setScale(tradeDecimals, RoundingMode.DOWN);
    }

}

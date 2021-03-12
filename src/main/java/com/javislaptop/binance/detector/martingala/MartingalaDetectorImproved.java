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

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaDetectorImproved {

    private static final Logger logger = LogManager.getLogger(MartingalaDetectorImproved.class);

    private final Binance binance;
    private final BinanceFormatter binanceFormatter;
    private final MartingalaProperties props;
    private final WalletState wallet;

    public MartingalaDetectorImproved(Binance binance, BinanceFormatter binanceFormatter, MartingalaProperties martingalaProperties, WalletState wallet) {
        this.binance = binance;
        this.binanceFormatter = binanceFormatter;
        this.props = martingalaProperties;
        this.wallet = wallet;
    }

    public void execute() {
        Map<String, List<Candlestick>> data = populate();

        int limit = data.values().stream().map(List::size).findAny().get();
        for (int i = 1; i < limit; i++) {
            for (String symbol : data.keySet()) {
                List<Candlestick> candlesticks = data.get(symbol);
                Candlestick c = candlesticks.get(i);
                Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);
                int purchases = 1;
                BigDecimal buyThreshold = updateBuyThreshold(c.getOpen(), tradeDecimals, purchases);
                while (c.getLow().compareTo(buyThreshold) < 0) {
                    wallet.buy(symbol, props.getBaseAmount(), buyThreshold, c.getCloseTime());
                    buyThreshold = updateBuyThreshold(buyThreshold, tradeDecimals, ++purchases);
                }
            }
            for (String symbol : data.keySet()) {
                Candlestick c = data.get(symbol).get(i);
                wallet.sell(symbol, c.getClose(), c.getCloseTime());
            }
        }
        wallet.printInfo();
    }

    private Map<String, List<Candlestick>> populate() {
        Instant from = props.getFrom().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = props.getTo().atStartOfDay(ZoneId.of("UTC")).toInstant();
        String interval = props.getInterval();
        String baseCurrency = props.getBaseCurrency();
        List<String> tradingCurrencies = props.getTradingCurrency();
        Map<String, List<Candlestick>> data = tradingCurrencies.stream().collect(Collectors.toMap(t -> t + baseCurrency, t -> binance.getCandlesticks(t + baseCurrency, from, to, interval)));
        if (data.values().stream().map(List::size).distinct().count() > 1) {
            throw new RuntimeException("The data is different for each pair, please improve the algorithm");
        }
        return data;
    }

    private BigDecimal updateBuyThreshold(BigDecimal referencePrice, Integer tradeDecimals, int attempts) {
        BigDecimal ratio = props.getDecrease();
        if (props.isAutomaticThreshold()) {
            ratio = ratio.multiply(new BigDecimal(attempts));
        }
        return referencePrice.subtract(referencePrice.multiply(ratio)).setScale(tradeDecimals, RoundingMode.DOWN);
    }

}

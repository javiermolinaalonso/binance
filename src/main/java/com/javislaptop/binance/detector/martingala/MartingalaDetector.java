package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.BinanceFormatter;
import com.javislaptop.binance.api.domain.Candlestick;
import com.javislaptop.binance.detector.pump.PumpInstantDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);

    private final Binance binance;
    private final BinanceFormatter binanceFormatter;
    private final MartingalaProperties props;
    private final WalletState wallet;

    public MartingalaDetector(Binance binance, BinanceFormatter binanceFormatter, MartingalaProperties martingalaProperties, WalletState wallet) {
        this.binance = binance;
        this.binanceFormatter = binanceFormatter;
        this.props = martingalaProperties;
        this.wallet = wallet;
    }

    public void execute() {
        List<String> tradingCurrencies = props.getTradingCurrency();
        String baseCurrency = props.getBaseCurrency();

        Instant from = props.getFrom().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = props.getTo().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant currentTo = from.plus(1, ChronoUnit.DAYS);

        while (from.compareTo(to) < 0) {

            for (String tradingCurrency : tradingCurrencies) {
                String symbol = tradingCurrency + baseCurrency;
                Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);

                List<Candlestick> candlesticks = binance.getMinuteBar(symbol, from, currentTo);

                BigDecimal openPrice = candlesticks.get(0).getOpen().setScale(tradeDecimals, RoundingMode.HALF_DOWN);
                BigDecimal sellPrice = candlesticks.get(candlesticks.size() - 1).getClose().setScale(tradeDecimals, RoundingMode.HALF_DOWN);

                BigDecimal buyThreshold = updateBuyThreshold(openPrice, tradeDecimals);
                for (Candlestick c : candlesticks) {
                    if (c.getLow().compareTo(buyThreshold) < 0) {
                        wallet.buy(symbol, props.getBaseAmount(), buyThreshold, c.getCloseTime());
                        buyThreshold = updateBuyThreshold(buyThreshold, tradeDecimals);
                    }
                }

                wallet.sell(symbol, sellPrice, currentTo);
            }
            from = from.plus(1, ChronoUnit.DAYS);
            currentTo = currentTo.plus(1, ChronoUnit.DAYS);
        }

        wallet.getTrades().stream().sorted(Comparator.comparing(Trade::getWhen)).forEach(logger::info);
        wallet.printInfo();
    }

    private BigDecimal updateBuyThreshold(BigDecimal referencePrice, Integer tradeDecimals) {
        return referencePrice.subtract(referencePrice.multiply(props.getDecrease()).divide(new BigDecimal(100), tradeDecimals, RoundingMode.HALF_DOWN)).setScale(tradeDecimals, RoundingMode.DOWN);
    }

}

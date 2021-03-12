package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.BinanceFormatter;
import com.javislaptop.binance.api.domain.Candlestick;
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
public class BinanceMartingalaDetector extends MartingalaDetector {

    private final Binance binance;
    private final BinanceFormatter binanceFormatter;

    public BinanceMartingalaDetector(BinanceFormatter binanceFormatter, MartingalaProperties martingalaProperties, WalletState wallet, Binance binance) {
        super(martingalaProperties, wallet);
        this.binance = binance;
        this.binanceFormatter = binanceFormatter;
    }

    @Override
    public Map<String, List<Candlestick>> populate() {
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

    @Override
    BigDecimal updateBuyThreshold(BigDecimal referencePrice, String symbol, int attempts) {
        Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);
        BigDecimal ratio = props.getDecrease();
        if (props.isAutomaticThreshold()) {
            ratio = ratio.multiply(new BigDecimal(attempts));
        }
        return referencePrice.subtract(referencePrice.multiply(ratio)).setScale(tradeDecimals, RoundingMode.DOWN);
    }
}

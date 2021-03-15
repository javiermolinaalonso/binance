package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.BinanceFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaRealtime {

    private static final Logger logger = LogManager.getLogger(MartingalaRealtime.class);

    private final Binance binance;
    private final BinanceFormatter binanceFormatter;
    private final MartingalaProperties props;

    private final Map<String, BigDecimal> thresholds;
    private final Map<String, Instant> whenToPurchase;

    public MartingalaRealtime(Binance binance, BinanceFormatter binanceFormatter, MartingalaProperties props) {
        this.binance = binance;
        this.binanceFormatter = binanceFormatter;
        this.props = props;
        this.thresholds = new HashMap<>();
        this.whenToPurchase = new HashMap<>();
    }

//    @PostConstruct
    public void init() {
        updateThresholds();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void dayReset() {
        logger.info("Resetting the trades");
        sell();
        updateThresholds();
    }

    private void updateThresholds() {
        getSymbols().forEach(symbol -> thresholds.put(symbol, updateBuyThreshold(symbol)));
    }

    private void sell() {
        props.getTradingCurrency().forEach(asset -> {
            BigDecimal assetBalance = new BigDecimal(binance.getAssetBalance(asset));
            if (assetBalance.compareTo(BigDecimal.ZERO) > 0) {
                String symbol = asset + props.getBaseCurrency();
                BigDecimal price = binance.getSellPrice(symbol);
//                binance.sellMarket(symbol, assetBalance);
                logger.info("Selling {}{} at {}", assetBalance, asset, price);
            }
        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void execute() {
        getSymbols().forEach(this::execute);
    }

    private void execute(String symbol) {
        Instant temporalRestrictionInstant = whenToPurchase.get(symbol);
        if (temporalRestrictionInstant != null && temporalRestrictionInstant.isAfter(Instant.now())) {
            logger.info("Skipping possible purchase for {}", symbol);
            return;
        }
        BigDecimal price = binance.getBuyPrice(symbol);
        if (price.compareTo(thresholds.get(symbol)) < 0) {
            logger.info("The price for {} went beyond the threshold", symbol);
            buyAndUpdateThreshold(symbol, price);
        }
    }

    private void buyAndUpdateThreshold(String symbol, BigDecimal price) {
        buy(symbol, price);
        updateBuyThreshold(symbol, price);
    }

    private void buy(String symbol, BigDecimal price) {
        logger.info("Purchasing {} at price {}", symbol, price);
        whenToPurchase.put(symbol, Instant.now().plus(props.getMinutesToBuyAgain(), ChronoUnit.MINUTES));
//        NewOrderResponse newOrderResponse = binance.buyLimit(symbol, props.getBaseAmount(), price);
        //TODO do something with this?
    }

    public List<String> getSymbols() {
        return props.getTradingCurrency().stream().map(c -> c + props.getBaseCurrency()).collect(Collectors.toList());
    }

    private BigDecimal updateBuyThreshold(String symbol) {
        BigDecimal referencePrice = binance.getSellPrice(symbol);
        return updateBuyThreshold(symbol, referencePrice);
    }

    private BigDecimal updateBuyThreshold(String symbol, BigDecimal referencePrice) {
        Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);
        BigDecimal ratio = props.getDecrease();
        BigDecimal threshold = referencePrice.subtract(referencePrice.multiply(ratio)).setScale(tradeDecimals, RoundingMode.DOWN);
        logger.info("Updated buy threshold for {} to {}", symbol, threshold);
        return threshold;
    }

}

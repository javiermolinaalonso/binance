package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
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
import java.util.List;
import java.util.OptionalDouble;

import static java.math.BigDecimal.valueOf;
import static java.util.stream.Collectors.joining;

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);

    private final Binance binance;
    private final MartingalaProperties martingalaProperties;

    public MartingalaDetector(Binance binance, MartingalaProperties martingalaProperties) {
        this.binance = binance;
        this.martingalaProperties = martingalaProperties;
    }

    public void execute() {
        String tradingCurrency = martingalaProperties.getTradingCurrency();
        String baseCurrency = martingalaProperties.getBaseCurrency();
        String symbol = tradingCurrency + baseCurrency;

        Instant from = martingalaProperties.getFrom().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = martingalaProperties.getTo().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant currentTo = from.plus(1, ChronoUnit.DAYS);

        List<BigDecimal> purchases = new ArrayList<>();
        List<BigDecimal> benefits = new ArrayList<>();
        BigDecimal baseAmount = martingalaProperties.getBaseAmount();
        BigDecimal originalAmount = martingalaProperties.getOriginalAmount();
        BigDecimal remainingAmount = originalAmount;

        BigDecimal commission = martingalaProperties.getComission();
        BigDecimal totalComission = BigDecimal.ZERO;

        BigDecimal initialPrice = null;
        BigDecimal finalPrice = null;
        BigDecimal maxAmountInRisk = BigDecimal.ZERO;
        LocalDate maxRiskDay = null;

        while (from.compareTo(to) < 0) {
            BigDecimal amountBeforeTrading = remainingAmount;
            List<Candlestick> candlesticks = binance.getMinuteBar(symbol, from, currentTo);

            BigDecimal referencePrice = candlesticks.get(0).getOpen();
            BigDecimal sellPrice = candlesticks.get(candlesticks.size() - 1).getClose();
            BigDecimal currentThreshold = calculateNewThreshold(referencePrice);
            if (initialPrice == null) {
                initialPrice = referencePrice;
            }
            finalPrice = sellPrice;

            int count = 1;
            BigDecimal purchasedAmount = BigDecimal.ZERO;
            BigDecimal spentBaseAmount = BigDecimal.ZERO;
            boolean ranOutOfMoney = false;
            for (Candlestick c : candlesticks) {
                if (c.getClose().compareTo(currentThreshold) < 0) {
                    purchases.add(currentThreshold);
                    BigDecimal spentBtc = baseAmount.multiply(new BigDecimal(count));
                    if (remainingAmount.compareTo(spentBtc) < 0) {
                        ranOutOfMoney = true;
                        spentBtc = remainingAmount;
                    }
                    spentBaseAmount = spentBaseAmount.add(spentBtc);
                    BigDecimal purchasedCoins = spentBtc.divide(currentThreshold, 8, RoundingMode.HALF_DOWN);
                    remainingAmount = remainingAmount.subtract(spentBtc);
                    purchasedAmount = purchasedAmount.add(purchasedCoins);
                    currentThreshold = calculateNewThreshold(currentThreshold);
                    count = count + martingalaProperties.getStepIncreases();
                }
            }

            logger.debug("Daily purchase {} {}", purchasedAmount, tradingCurrency);
            logger.debug("Remaining before selling {} {}", remainingAmount, baseCurrency);

            LocalDate when = LocalDate.ofInstant(from, ZoneId.of("UTC"));
            if (purchasedAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal btcAfterSell = purchasedAmount.multiply(sellPrice);
                remainingAmount = remainingAmount.add(btcAfterSell);
                BigDecimal benefitPercent = remainingAmount.divide(amountBeforeTrading, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100));
                BigDecimal averagePrice = spentBaseAmount.divide(purchasedAmount, 10, RoundingMode.HALF_DOWN);
                logger.debug("Base currency after selling {} {}", remainingAmount, baseCurrency);
                BigDecimal spentComission = spentBaseAmount.add(btcAfterSell).multiply(commission);
                totalComission = totalComission.add(spentComission);

                if (spentBaseAmount.compareTo(maxAmountInRisk) > 0) {
                    maxRiskDay = LocalDate.ofInstant(from, ZoneId.of("UTC"));
                    maxAmountInRisk = spentBaseAmount;
                }

                logger.info("[{}] Purchased {} {} at average price of {}. Sold them at price {} {}. Expected comission of {} {}. Benefit without commissions of {}%", when, purchasedAmount, tradingCurrency, averagePrice, sellPrice, baseCurrency, spentComission, baseCurrency, benefitPercent);
                if (ranOutOfMoney) {
                    logger.info("[{}] You ran out of money today", when);
                }

                logger.debug("Purchases {}", purchases.stream().map(BigDecimal::toString).collect(joining(",", "[", "]")));
                logger.debug("Sell at {}", sellPrice);
                OptionalDouble average = purchases.stream().mapToDouble(BigDecimal::doubleValue).average();
                if (average.isPresent()) {
                    BigDecimal precisionNum = valueOf(average.getAsDouble());
                    BigDecimal benefit = sellPrice.divide(precisionNum, 8, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE);
                    benefits.add(benefit);
                    logger.debug("Aprox benefit: {}. Purchases: {}", benefit, count);
                }
            } else {
                logger.info("[{}] No trade today", when);
            }

            from = from.plus(1, ChronoUnit.DAYS);
            currentTo = currentTo.plus(1, ChronoUnit.DAYS);
            purchases = new ArrayList<>();
        }
        double periodBenefit = benefits.stream().mapToDouble(BigDecimal::doubleValue).sum() * 100;
        logger.info("Theorical benefit of period {}%", periodBenefit);

        logger.info("Summary for {}{} between {} and {}. Starting with {} {} with base trades of {}{} and doing steps of {}.",
                tradingCurrency, baseCurrency, martingalaProperties.getFrom(), martingalaProperties.getTo(), martingalaProperties.getOriginalAmount(), martingalaProperties.getBaseCurrency(), martingalaProperties.getBaseAmount(), martingalaProperties.getBaseCurrency(), martingalaProperties.getStepIncreases());
        logger.info("The initial price for the period was {} and the final was {}. An increase of {}%", initialPrice, finalPrice, finalPrice.divide(initialPrice, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        BigDecimal amountWithComission = remainingAmount.subtract(totalComission);
        logger.info("Original amount was {} {} and now have {} {}. An increase of {}%", originalAmount, baseCurrency, amountWithComission, baseCurrency, amountWithComission.divide(originalAmount, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        logger.info("The maximum risk was {} {} the day {}", maxAmountInRisk, baseCurrency, maxRiskDay);
        logger.info("The applied comission is {} {}", totalComission, baseCurrency);
    }

    private BigDecimal calculateNewThreshold(BigDecimal referencePrice) {
        return referencePrice.subtract(referencePrice.multiply(martingalaProperties.getDecrease()).divide(new BigDecimal(100), 8, RoundingMode.HALF_DOWN));
    }

}

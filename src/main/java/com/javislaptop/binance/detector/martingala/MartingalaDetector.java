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
    private final MartingalaProperties props;

    public MartingalaDetector(Binance binance, MartingalaProperties martingalaProperties) {
        this.binance = binance;
        this.props = martingalaProperties;
    }

    public void execute() {
        String tradingCurrency = props.getTradingCurrency();
        String baseCurrency = props.getBaseCurrency();
        String symbol = tradingCurrency + baseCurrency;

        Instant from = props.getFrom().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = props.getTo().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant currentTo = from.plus(1, ChronoUnit.DAYS);

        BigDecimal baseAmount = props.getBaseAmount();
        BigDecimal originalAmount = props.getOriginalAmount();
        BigDecimal remainingAmount = originalAmount;

        BigDecimal commission = props.getComission();
        BigDecimal totalComission = BigDecimal.ZERO;

        BigDecimal initialPrice = null;
        BigDecimal finalPrice = null;
        BigDecimal maxAmountInRisk = BigDecimal.ZERO;
        LocalDate maxRiskDay = null;

        while (from.compareTo(to) < 0) {
            LocalDate when = LocalDate.ofInstant(from, ZoneId.of("UTC"));

            BigDecimal amountBeforeTrading = remainingAmount;
            List<Candlestick> candlesticks = binance.getMinuteBar(symbol, from, currentTo);

            BigDecimal referencePrice = candlesticks.get(0).getOpen();
            BigDecimal sellPrice = candlesticks.get(candlesticks.size() - 1).getClose();
            BigDecimal buyThreshold = calculateBuyThreshold(referencePrice);
            BigDecimal sellThreshold = calculateSellThreshold(referencePrice);
            if (initialPrice == null) {
                initialPrice = referencePrice;
            }
            finalPrice = sellPrice;

            int count = 1;
            BigDecimal purchasedAmount = BigDecimal.ZERO;
            BigDecimal spentBaseAmount = BigDecimal.ZERO;
            boolean ranOutOfMoney = false;
            boolean first = props.isPurchaseBeginningDay();
            for (Candlestick c : candlesticks) {
                if (first || c.getLow().compareTo(buyThreshold) < 0) {
                    BigDecimal spentBtc = baseAmount.multiply(new BigDecimal(count));
                    if (remainingAmount.compareTo(spentBtc) < 0) {
                        ranOutOfMoney = true;
                        spentBtc = remainingAmount;
                    }
                    if (spentBaseAmount.add(spentBtc).compareTo(originalAmount) > 0) {
                        ranOutOfMoney = true;
                        spentBtc = spentBaseAmount.subtract(originalAmount);
                    }
                    if (spentBtc.compareTo(BigDecimal.ZERO) > 0) {
                        spentBaseAmount = spentBaseAmount.add(spentBtc);
                        BigDecimal purchasedCoins = spentBtc.divide(buyThreshold, 8, RoundingMode.HALF_DOWN);
                        remainingAmount = remainingAmount.subtract(spentBtc);
                        purchasedAmount = purchasedAmount.add(purchasedCoins);
                        buyThreshold = calculateBuyThreshold(buyThreshold);
                        count = count + props.getStepIncreases();
                    }
                } else if (props.getIncrease().compareTo(BigDecimal.ZERO) > 0 && c.getHigh().compareTo(sellThreshold) > 0 && purchasedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal btcAfterSell = purchasedAmount.multiply(sellThreshold);
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

                    logger.debug("[{}] Purchased {} {} at average price of {}. Sold them at price {} {}. Expected comission of {} {}. Benefit without commissions of {}%", when, purchasedAmount, tradingCurrency, averagePrice, sellThreshold, baseCurrency, spentComission, baseCurrency, benefitPercent);
                    if (ranOutOfMoney) {
                        logger.info("[{}] You ran out of money today", when);
                    }

                    ranOutOfMoney = false;
                    purchasedAmount = BigDecimal.ZERO;
                    spentBaseAmount = BigDecimal.ZERO;
                    amountBeforeTrading = remainingAmount;
                    buyThreshold = calculateBuyThreshold(sellThreshold);
                    sellThreshold = calculateSellThreshold(sellThreshold);
                }
                first = false;
            }

            logger.debug("Daily purchase {} {}", purchasedAmount, tradingCurrency);
            logger.debug("Remaining before selling {} {}", remainingAmount, baseCurrency);


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

                logger.debug("[{}] Purchased {} {} at average price of {}. Sold them at price {} {}. Expected comission of {} {}. Benefit without commissions of {}%", when, purchasedAmount, tradingCurrency, averagePrice, sellPrice, baseCurrency, spentComission, baseCurrency, benefitPercent);
                if (ranOutOfMoney) {
                    logger.info("[{}] You ran out of money today", when);
                }
            }

            from = from.plus(1, ChronoUnit.DAYS);
            currentTo = currentTo.plus(1, ChronoUnit.DAYS);
        }

        logger.info("Summary for {}{} between {} and {}. Starting with {} {} with base trades of {}{} and doing steps of {}.",
                tradingCurrency, baseCurrency, props.getFrom(), props.getTo(), props.getOriginalAmount(), props.getBaseCurrency(), props.getBaseAmount(), props.getBaseCurrency(), props.getStepIncreases());
        logger.info("The initial price for the period was {} and the final was {}. An increase of {}%", initialPrice, finalPrice, finalPrice.divide(initialPrice, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        BigDecimal amountWithComission = remainingAmount.subtract(totalComission);
        logger.info("Original amount was {} {} and now have {} {}. An increase of {}%", originalAmount, baseCurrency, amountWithComission, baseCurrency, amountWithComission.divide(originalAmount, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        logger.info("The maximum risk was {} {} the day {}", maxAmountInRisk, baseCurrency, maxRiskDay);
        logger.info("The applied comission is {} {}", totalComission, baseCurrency);
    }

    private BigDecimal calculateBuyThreshold(BigDecimal referencePrice) {
        return referencePrice.subtract(referencePrice.multiply(props.getDecrease()).divide(new BigDecimal(100), 8, RoundingMode.HALF_DOWN));
    }
    private BigDecimal calculateSellThreshold(BigDecimal referencePrice) {
        return referencePrice.add(referencePrice.multiply(props.getIncrease()).divide(new BigDecimal(100), 8, RoundingMode.HALF_DOWN));
    }

}

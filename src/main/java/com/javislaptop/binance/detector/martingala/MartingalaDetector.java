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
import java.util.List;


@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);

    private final Binance binance;
    private final BinanceFormatter binanceFormatter;
    private final MartingalaProperties props;

    public MartingalaDetector(Binance binance, BinanceFormatter binanceFormatter, MartingalaProperties martingalaProperties) {
        this.binance = binance;
        this.binanceFormatter = binanceFormatter;
        this.props = martingalaProperties;
    }

    public void execute() {
        String tradingCurrency = props.getTradingCurrency();
        String baseCurrency = props.getBaseCurrency();
        String symbol = tradingCurrency + baseCurrency;

        Integer tradeDecimals = binanceFormatter.getTradeDecimals(symbol);
        Integer tradingCurrencyPrecision = binanceFormatter.getBaseAssetPrecision(symbol);
        Integer baseCurrencyPrecision = binanceFormatter.getQuotePrecision(symbol);

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
        BigDecimal tradedVolume = BigDecimal.ZERO;
        BigDecimal benefitOverRiskTotal = BigDecimal.ZERO;

        List<Trade> trades = new ArrayList<>();
        while (from.compareTo(to) < 0) {
            LocalDate when = LocalDate.ofInstant(from, ZoneId.of("UTC"));

            BigDecimal amountBeforeTrading = remainingAmount;
            List<Candlestick> candlesticks = binance.getMinuteBar(symbol, from, currentTo);

            BigDecimal referencePrice = candlesticks.get(0).getOpen();
            BigDecimal sellPrice = candlesticks.get(candlesticks.size() - 1).getClose().setScale(tradeDecimals, RoundingMode.HALF_DOWN);

            BigDecimal buyThreshold = calculateBuyThreshold(referencePrice, tradeDecimals);
            BigDecimal sellThreshold = calculateSellThreshold(referencePrice, tradeDecimals);
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
                    BigDecimal spentBase = baseAmount.multiply(new BigDecimal(count)).setScale(baseCurrencyPrecision, RoundingMode.DOWN);
                    if (remainingAmount.compareTo(spentBase) < 0) {
                        ranOutOfMoney = true;
                        spentBase = remainingAmount;
                    }
                    if (spentBaseAmount.add(spentBase).compareTo(originalAmount) > 0) {
                        ranOutOfMoney = true;
                        spentBase = spentBaseAmount.subtract(originalAmount);
                    }
                    if (spentBase.compareTo(BigDecimal.ZERO) > 0) {
                        tradedVolume = tradedVolume.add(spentBase);
                        spentBaseAmount = spentBaseAmount.add(spentBase);
                        BigDecimal purchasedCoins = spentBase.divide(buyThreshold, tradingCurrencyPrecision, RoundingMode.DOWN).setScale(tradingCurrencyPrecision, RoundingMode.DOWN);
                        remainingAmount = remainingAmount.subtract(spentBase);
                        purchasedAmount = purchasedAmount.add(purchasedCoins);

                        trades.add(new Trade(c.getCloseTime(), purchasedCoins, buyThreshold, symbol, "BUY"));
                        buyThreshold = calculateBuyThreshold(buyThreshold, tradeDecimals);
                        count = count + props.getStepIncreases();
                    }
                } else if (props.getIncrease().compareTo(BigDecimal.ZERO) > 0 && c.getHigh().compareTo(sellThreshold) > 0 && purchasedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal baseCoinAfterSell = purchasedAmount.multiply(sellThreshold).setScale(baseCurrencyPrecision, RoundingMode.DOWN);
                    tradedVolume = tradedVolume.add(baseCoinAfterSell);
                    remainingAmount = remainingAmount.add(baseCoinAfterSell);
                    BigDecimal averagePrice = spentBaseAmount.divide(purchasedAmount, tradeDecimals, RoundingMode.HALF_DOWN).setScale(tradeDecimals, RoundingMode.DOWN);
                    BigDecimal spentComission = spentBaseAmount.add(baseCoinAfterSell).multiply(commission).setScale(baseCurrencyPrecision, RoundingMode.HALF_DOWN);
                    BigDecimal benefitPercent = remainingAmount.subtract(spentComission).divide(amountBeforeTrading, 2, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100));
                    totalComission = totalComission.add(spentComission);
                    if (spentBaseAmount.compareTo(maxAmountInRisk) > 0) {
                        maxRiskDay = LocalDate.ofInstant(from, ZoneId.of("UTC"));
                        maxAmountInRisk = spentBaseAmount;
                    }
                    BigDecimal benefitOverBetMoney = baseCoinAfterSell.subtract(spentComission).divide(spentBaseAmount, baseCurrencyPrecision, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100));
                    trades.add(new Trade(c.getCloseTime(), averagePrice, sellThreshold, symbol, "SELL"));
                    logger.info("[{}] Purchased {} {} at average price of {}. Sold them at price {} {}. Expected comission of {} {}. Benefit with commissions of {}%. Benefit over risk is {} %", when, purchasedAmount, tradingCurrency, averagePrice, sellPrice, baseCurrency, spentComission, baseCurrency, benefitPercent, benefitOverBetMoney);
                    if (ranOutOfMoney) {
                        logger.debug("[{}] You ran out of money today", when);
                    }

                    ranOutOfMoney = false;
                    purchasedAmount = BigDecimal.ZERO;
                    spentBaseAmount = BigDecimal.ZERO;
                    amountBeforeTrading = remainingAmount;
                    buyThreshold = calculateBuyThreshold(sellThreshold, tradeDecimals);
                    sellThreshold = calculateSellThreshold(sellThreshold, tradeDecimals);
                }
                first = false;
            }

            logger.debug("Daily purchase {} {}", purchasedAmount, tradingCurrency);
            logger.debug("Remaining before selling {} {}", remainingAmount, baseCurrency);

            if (purchasedAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal baseCoinAfterSell = purchasedAmount.multiply(sellPrice).setScale(baseCurrencyPrecision, RoundingMode.DOWN);
                tradedVolume = tradedVolume.add(baseCoinAfterSell);
                remainingAmount = remainingAmount.add(baseCoinAfterSell);
                BigDecimal averagePrice = spentBaseAmount.divide(purchasedAmount, tradeDecimals, RoundingMode.HALF_DOWN).setScale(tradeDecimals, RoundingMode.DOWN);;
                BigDecimal spentComission = spentBaseAmount.add(baseCoinAfterSell).multiply(commission).setScale(baseCurrencyPrecision, RoundingMode.HALF_DOWN);
                BigDecimal benefitPercent = remainingAmount.subtract(spentComission).divide(amountBeforeTrading, 8, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
                BigDecimal benefitOverBetMoney = baseCoinAfterSell.subtract(spentComission).divide(spentBaseAmount, baseCurrencyPrecision, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100));
                benefitOverRiskTotal = benefitOverRiskTotal.add(benefitOverBetMoney);
                totalComission = totalComission.add(spentComission).setScale(baseCurrencyPrecision, RoundingMode.HALF_DOWN);

                if (spentBaseAmount.compareTo(maxAmountInRisk) > 0) {
                    maxRiskDay = LocalDate.ofInstant(from, ZoneId.of("UTC"));
                    maxAmountInRisk = spentBaseAmount;
                }
                trades.add(new Trade(currentTo, purchasedAmount, sellPrice, symbol, "SELL"));
                logger.debug("[{}] Purchased {} {} at average price of {}. Sold them at price {} {}. Expected comission of {} {}. Benefit with commissions of {}%. Benefit over risk is {} %", when, purchasedAmount, tradingCurrency, averagePrice, sellPrice, baseCurrency, spentComission, baseCurrency, benefitPercent, benefitOverBetMoney);
                if (ranOutOfMoney) {
                    logger.info("[{}] You ran out of money today", when);
                }
            }

            from = from.plus(1, ChronoUnit.DAYS);
            currentTo = currentTo.plus(1, ChronoUnit.DAYS);
        }

        trades.forEach(logger::info);
        logger.info("Summary for {}{} between {} and {}. Starting with {} {} with base trades of {}{}.",
                tradingCurrency, baseCurrency, props.getFrom(), props.getTo(), props.getOriginalAmount(), props.getBaseCurrency(), props.getBaseAmount(), props.getBaseCurrency());
        logger.info("The initial price for the period was {} and the final was {}. An increase of {}%", initialPrice, finalPrice, finalPrice.divide(initialPrice, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        BigDecimal amountWithComission = remainingAmount.subtract(totalComission);
        logger.info("Original amount was {} {} and now have {} {}. An increase of {}%", originalAmount, baseCurrency, amountWithComission, baseCurrency, amountWithComission.divide(originalAmount, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        logger.info("The benefit over risk is {}%", benefitOverRiskTotal);
        logger.info("The maximum risk was {} {} the day {}", maxAmountInRisk, baseCurrency, maxRiskDay);
        logger.info("The applied comission is {} {}", totalComission, baseCurrency);
        logger.info("The traded volume is {} {}", tradedVolume, baseCurrency);

    }

    private BigDecimal calculateBuyThreshold(BigDecimal referencePrice, Integer tradeDecimals) {
        return referencePrice.subtract(referencePrice.multiply(props.getDecrease()).divide(new BigDecimal(100), tradeDecimals, RoundingMode.HALF_DOWN)).setScale(tradeDecimals, RoundingMode.DOWN);
    }
    private BigDecimal calculateSellThreshold(BigDecimal referencePrice, Integer tradeDecimals) {
        return referencePrice.add(referencePrice.multiply(props.getIncrease()).divide(new BigDecimal(100), tradeDecimals, RoundingMode.UP)).setScale(tradeDecimals, RoundingMode.UP);
    }

}

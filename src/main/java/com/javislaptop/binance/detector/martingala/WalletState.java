package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.BinanceFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Service
public class WalletState {

    private static final Logger logger = LogManager.getLogger(WalletState.class);

    private final MartingalaProperties props;

    private final BigDecimal spendLimit;
    private final String baseCurrency;
    private final List<Trade> trades;
    private final BinanceFormatter binanceFormatter;

    private BigDecimal baseCoins;
    private Map<String, BigDecimal> tradeCoins;
    private BigDecimal tradedVolume;
    private BigDecimal totalFee;

    private Set<Instant> daysRanOutOfMoney;

    public WalletState(MartingalaProperties props, BinanceFormatter binanceFormatter) {
        this.props = props;
        this.spendLimit = props.getOriginalAmount();
        this.baseCurrency = props.getBaseCurrency();
        this.binanceFormatter = binanceFormatter;
        trades = new ArrayList<>();
        baseCoins = spendLimit;
        tradeCoins = new HashMap<>();
        totalFee = BigDecimal.ZERO;
        tradedVolume = BigDecimal.ZERO;
        this.daysRanOutOfMoney = new TreeSet<>();
    }

    public Optional<Trade> buy(String symbol, BigDecimal tradeBaseCoins, BigDecimal price, Instant when) {
        if (baseCoins.compareTo(tradeBaseCoins) < 0) {
            daysRanOutOfMoney.add(when);
            return Optional.empty();
        }
        if (props.isLimitToInitialInvestment() && getInvestedAmount(when).compareTo(spendLimit) > 0) {
            daysRanOutOfMoney.add(when);
            return Optional.empty();
        }
        String tradeCoin = symbol.replaceAll(baseCurrency, "");
        BigDecimal fee = computeFee(tradeBaseCoins);
        BigDecimal purchasedCoins = tradeBaseCoins.subtract(fee).divide(price, binanceFormatter.getBaseAssetPrecision(symbol), RoundingMode.DOWN);
        tradeCoins.put(tradeCoin, tradeCoins.getOrDefault(tradeCoin, BigDecimal.ZERO).add(purchasedCoins));
        baseCoins = baseCoins.subtract(tradeBaseCoins);
        tradedVolume = tradedVolume.add(tradeBaseCoins);
        return addTrade(symbol, price, when, purchasedCoins, "BUY");
    }

    public void sell(String symbol, BigDecimal sellPrice, Instant when) {
        String tradeCoin = symbol.replaceAll(baseCurrency, "");
        BigDecimal amountOfTradeCoins = tradeCoins.getOrDefault(tradeCoin, BigDecimal.ZERO);
        if (amountOfTradeCoins.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal baseCoinAfterSell = amountOfTradeCoins.multiply(sellPrice).setScale(binanceFormatter.getQuotePrecision(symbol), RoundingMode.DOWN);
            BigDecimal fee = computeFee(baseCoinAfterSell);
            baseCoinAfterSell = baseCoinAfterSell.subtract(fee);
            baseCoins = baseCoins.add(baseCoinAfterSell);
            tradeCoins.put(tradeCoin, BigDecimal.ZERO);
            addTrade(symbol, sellPrice, when, amountOfTradeCoins, "SELL");
        }
    }

    private Optional<Trade> addTrade(String symbol, BigDecimal price, Instant when, BigDecimal tradedCoins, String direction) {
        Trade trade = new Trade(when, tradedCoins, price, symbol, direction);
        trades.add(trade);
        return of(trade);
    }

    private BigDecimal computeFee(BigDecimal tradeBaseCoins) {
        BigDecimal comission = tradeBaseCoins.multiply(props.getComission());
        totalFee = totalFee.add(comission);
        return comission;
    }

    public BigDecimal getTradedVolume() {
        return tradedVolume;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void printInfo() {
        String tradingCurrencies = tradeCoins.keySet().toString();
        TreeMap<Instant, List<Trade>> periodTrades = trades.stream().collect(Collectors.groupingBy(Trade::getWhen, TreeMap::new, toList()));
        periodTrades.forEach((key, value) -> {
            BigDecimal investedAmount = getInvestedAmount(value);
            BigDecimal receivedAmount = value.stream().filter(t -> t.getDirection().equals("SELL")).map(t -> t.getAmount().multiply(t.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal benefit = receivedAmount.divide(investedAmount, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
            String ranOutMoney = "";
            if (daysRanOutOfMoney.contains(key)) {
                ranOutMoney = "Ran out of money";
            }
            logger.info("The benefit for the period {} is {}%. Spent amount was: {}. {}", key.truncatedTo(ChronoUnit.MINUTES), benefit, investedAmount.setScale(2, RoundingMode.HALF_DOWN), ranOutMoney);
        });
        logger.info("Summary for {}{} between {} and {}. Starting with {} {} with base trades of {}{}.",
                tradingCurrencies, baseCurrency, props.getFrom(), props.getTo(), props.getOriginalAmount(), props.getBaseCurrency(), props.getBaseAmount(), props.getBaseCurrency());
        logger.info("Original amount was {} {} and now have {} {}. An increase of {}%", spendLimit, baseCurrency, baseCoins, baseCurrency, baseCoins.divide(spendLimit, 4, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE).multiply(new BigDecimal(100)));
        logger.info("The applied comission is {} {}", totalFee, baseCurrency);
        logger.info("The traded volume is {} {}", tradedVolume, baseCurrency);
//        SortedMap<Instant, List<Trade>> periodTrades = trades.stream().collect(Collectors.groupingBy(Trade::getWhen));

    }

    private BigDecimal getInvestedAmount(Instant when) {
        return getInvestedAmount(trades.stream().filter(t -> t.getWhen().compareTo(when) == 0).collect(toList()));
    }

    private BigDecimal getInvestedAmount(List<Trade> value) {
        return value.stream().filter(t -> t.getDirection().equals("BUY")).map(t -> t.getAmount().multiply(t.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

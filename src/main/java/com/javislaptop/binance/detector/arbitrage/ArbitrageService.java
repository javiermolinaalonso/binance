package com.javislaptop.binance.detector.arbitrage;

import com.javislaptop.binance.api.AccountService;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.orderbook.MonitorOrderBookTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.stream.Collectors.groupingBy;

@Service
public class ArbitrageService {

    private static final Logger logger = LogManager.getLogger(ArbitrageService.class);

    private final Binance binance;
    private final AccountService accountService;

    private final Map<String, List<String>> pairs = Map.of(
            "CHRBTC", List.of("CHRBNB", "BNBBTC")
//            "SCBTC", List.of("SCUSDT", "BTCUSDT")
//            "COSBTC", List.of("COSUSDT", "BTCUSDT"),
//            "CELRBTC", List.of("CELRUSDT", "BTCUSDT"),
//            "NBSBTC", List.of("NBSUSDT", "BTCUSDT")
    );

    public ArbitrageService(Binance binance, AccountService accountService) {
        this.binance = binance;
        this.accountService = accountService;
    }

    public void execute() {
//        pairs.forEach(
//                (pair, sells) -> {
//                    ArbitrageTask placeOrderTask = new ArbitrageTask(accountService, binance, pair, sells);
//                    new Timer("arbitrage-task-"+pair).schedule(placeOrderTask, 1000, 5000);
//                }
//        );


        pairs.forEach(
                (pair, sells) -> {
                    BigDecimal initialExpectedPrice = binance.getOrderBook(pair, 5).getBid();

                    BigDecimal amountOfCoins = BigDecimal.ONE.divide(initialExpectedPrice, 0, RoundingMode.DOWN);
                    logger.info("The ask price for {} is {}.", pair, initialExpectedPrice.toPlainString());

                    String s1 = sells.get(0);
                    BigDecimal bid = binance.getOrderBook(s1, 5).getBid();
                    BigDecimal amount = amountOfCoins.multiply(bid);
                    logger.info("The bid price for {} is {}.", s1, bid);

                    if (sells.size() > 1) {
                        String s2 = sells.get(1);
                        BigDecimal bid2 = binance.getOrderBook(s2, 5).getBid();
                        logger.info("The bid price for {} is {}.", s2, bid2);
                        BigDecimal amountOfBtc = amount.multiply(bid2).setScale(8, RoundingMode.HALF_DOWN);
                        logger.info("Expected amount of btc is {}", amountOfBtc);
                    }
                }
        );

        System.exit(0);
    }


}

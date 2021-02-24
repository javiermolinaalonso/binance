package com.javislaptop.binance.detector.arbitrage;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.general.FilterType;
import com.javislaptop.binance.api.AccountService;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import org.apache.juli.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.TimerTask;

public class ArbitrageTask extends TimerTask {

    private static final Logger logger = LogManager.getLogger(ArbitrageTask.class);

    private final BigDecimal amount = new BigDecimal("0.0005");

    private final AccountService accountService;
    private final Binance binance;
    private final String pair;
    private final List<String> sells;
    private final String coin;

    public ArbitrageTask(AccountService accountService, Binance binance, String pair, List<String> sells) {
        this.accountService = accountService;
        this.binance = binance;
        this.pair = pair;
        this.sells = sells;
        this.coin = pair.replaceAll("BTC", "");
    }

    @Override
    public void run() {
        BigDecimal freeBalance = new BigDecimal(accountService.getAccount().getAssetBalance(coin).getFree());
        BigDecimal lockedBalance = new BigDecimal(accountService.getAccount().getAssetBalance(coin).getLocked());

        if (lockedBalance.compareTo(BigDecimal.ZERO) == 0) {
            if (freeBalance.compareTo(BigDecimal.ZERO) > 0) {
                Order order = binance.sellMarket(sells.get(0), freeBalance);
                if (order.getStatus() == OrderStatus.FILLED) {
                    binance.buyMarket(sells.get(1), new BigDecimal(order.getCummulativeQuoteQty()));
                }
            } else {
                if (CollectionUtils.isEmpty(accountService.getOpenOrders(pair))) {
                    BinanceOrderBook book = binance.getOrderBook(pair, 5);
                    binance.buyLimit(pair, amount, book.getBid());
                }
            }
        }
    }
}

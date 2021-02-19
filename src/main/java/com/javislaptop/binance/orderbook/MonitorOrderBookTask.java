package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.AccountService;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;

public class MonitorOrderBookTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(MonitorOrderBookTask.class);
    private static final BigDecimal MIN_GAP = new BigDecimal("0.005");
    private static final BigDecimal BTC_PER_TRADE = new BigDecimal("0.0005");

    private final AccountService accountService;
    private final String symbol;
    private final Binance binance;
    private final SymbolInfo symbolInfo;
    private final BigDecimal stepSize;
    private final String coin;

    public MonitorOrderBookTask(AccountService accountService, SymbolInfo symbolInfo, Binance binance) {
        this.accountService = accountService;
        this.symbol = symbolInfo.getSymbol();
        this.binance = binance;
        this.symbolInfo = symbolInfo;
        this.stepSize = new BigDecimal(this.symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize());
        this.coin = symbol.replaceAll("BTC", "");
    }

    @Override
    public void run() {
        try {
            AssetBalance assetBalance = accountService.getAccount().getAssetBalance(coin);
            BinanceOrderBook book = binance.getOrderBook(symbol, 5);

            BigDecimal askPrice = book.getAsk();
            if (new BigDecimal(assetBalance.getFree()).compareTo(BigDecimal.ZERO) > 0) {
                //An execution has happened, require to sell
                binance.sellLimit(symbol, assetBalance.getFree(), askPrice);
            } else {
                List<Order> openOrders = accountService.getOpenOrders(symbol);
                if (new BigDecimal(assetBalance.getLocked()).compareTo(BigDecimal.ZERO) > 0) {
                    //A sell order has been placed, require to update?
                    Optional<Order> sellOrder = openOrders.stream().filter(o -> o.getSide() == OrderSide.SELL).findAny();
                    if (sellOrder.isPresent()) {
                        Order order = sellOrder.get();
                        BigDecimal limit = new BigDecimal(order.getPrice()).subtract(stepSize);
                        if (askPrice.compareTo(limit) < 0) {
                            logger.info("Oh oh the order book has lowered the price");
                            binance.cancelOrder(order);
                            binance.buyLimit(symbol, BTC_PER_TRADE, book.getBid().subtract(stepSize));
                        } else {
                            logger.debug("Nothing changed");
                        }
                    } else {
                        logger.info("There is amount locked but there are no sell orders?");
                    }
                } else {
                    //An order has been not placed or not executed
                    if (openOrders.isEmpty()) {
                        //An order shall be placed, but consider sometimes the get open orders fail...
                        if (book.getGap().compareTo(MIN_GAP) > 0) {
                            binance.buyLimit(symbol, BTC_PER_TRADE, book.getBid());
                        } else {
                            logger.info("The gap for {} is too small", symbol);
                            cancel();
                        }
                    } else {
                        Optional<Order> buyOrder = openOrders.stream().filter(o -> o.getSide() == OrderSide.BUY).findAny();
                        if (buyOrder.isPresent()) {
                            Order order = buyOrder.get();
                            BigDecimal limit = new BigDecimal(order.getPrice()).add(stepSize);
                            if (book.getBid().compareTo(limit) > 0){
                                logger.info("Oh oh the order book has increased the price");
                                binance.cancelOrder(order);
                                binance.buyLimit(symbol, BTC_PER_TRADE, book.getBid().subtract(stepSize));
                            }else {
                                logger.debug("Nothing changed");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cancel() {
        return super.cancel();
    }
}

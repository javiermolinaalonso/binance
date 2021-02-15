package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorOrderBookTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(MonitorOrderBookTask.class);

    private final Order order;
    private final StreamDataStorage storage;
    private final String symbol;
    private final Binance binance;
    private final BinanceDataStreamer streamer;

    private boolean purchased = false;


    public MonitorOrderBookTask(Order order, StreamDataStorage storage, String symbolInfo, Binance binance, BinanceDataStreamer streamer) {
        this.order = order;
        this.storage = storage;
        this.symbol = symbolInfo;
        this.binance = binance;
        this.streamer = streamer;
        streamer.enableBookTickerEvents(symbolInfo);
    }

    @Override
    public void run() {
        Optional<BinanceOrderBook> orderBook = storage.getDepth(symbol);
        orderBook.ifPresent(ticker -> {
            List<Order> openOrders = binance.getOpenOrders(symbol);
            Optional<Order> openOrder = openOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.FILLED)
                    .findAny();
            openOrder.ifPresent();
            purchaseOrder.ifPresent(o -> {
                o.get
            });
            if (purchaseOrder.isPresent()) {
                if ()
            }
            if (!purchased) {
                if (new BigDecimal(ticker.getAskPrice()).compareTo(order.getBuyPrice()) <= 0) {
                    logger.info("Purchased {} at {}", ticker.getSymbol(), order.getBuyPrice());
                    purchased = true;
                }
            } else {
                if (new BigDecimal(ticker.getBidPrice()).compareTo(order.getProfitPrice()) >= 0) {
                    logger.info("Sold {} at {}", ticker.getSymbol(), order.getProfitPrice());
                    logger.info("BENEFIT: {}", order.getProfitPercent());
                    this.cancel();
                } else if (new BigDecimal(ticker.getAskPrice()).compareTo(order.getLossPrice())<=0) {
                    logger.info("Sold {} at {}", ticker.getSymbol(), order.getLossPrice());
                    logger.info("LOSS: {}", order.getLossPercent());
                    this.cancel();
                }
            }
        });
    }

    @Override
    public boolean cancel() {
        TimerTask placeOrderTask = new PlaceOrderTask(binance, symbol, storage, streamer);
        new Timer("update-task-"+ symbol).schedule(placeOrderTask, 100, 1000);
        return super.cancel();
    }
}

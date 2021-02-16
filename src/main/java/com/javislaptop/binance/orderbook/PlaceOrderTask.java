package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

public class PlaceOrderTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(PlaceOrderTask.class);

    private static final BigDecimal RESISTANCE_THRESHOLD = new BigDecimal("0.05");
    private static final BigDecimal MIN_GAP = new BigDecimal("0.005");
    private static final BigDecimal MAX_GAP = new BigDecimal("0.05");
    private static final BigDecimal BTC_PER_TRADE = new BigDecimal("0.0005");

    private final Binance binance;
    private final String symbol;
    private final StreamDataStorage storage;
    private final BinanceDataStreamer streamer;

    public PlaceOrderTask(Binance binance, String symbol, StreamDataStorage storage, BinanceDataStreamer streamer) {
        this.binance = binance;
        this.symbol = symbol;
        this.storage = storage;
        this.streamer = streamer;
        logger.debug("Starting to watch {}", symbol);
    }

    @Override
    public void run() {
        logger.debug("Processing order book for {}", symbol);
        BinanceOrderBook orderBook = binance.getOrderBook(symbol, 500);

        orderBook.getDistanceBetweenFloorAndResistance(RESISTANCE_THRESHOLD)
                .filter(d -> d.compareTo(MIN_GAP) > 0)
                .filter(d -> d.compareTo(MAX_GAP) < 0)
                .ifPresent(s -> {
                    Order order = placeLimitOrder(binance.getSymbolInfo(symbol), orderBook.getFloor(RESISTANCE_THRESHOLD).get());

                    MonitorOrderBookTask monitorOrderBookTask = new MonitorOrderBookTask(storage, symbol, binance, streamer);
                    new Timer("monitorbook-task-"+ symbol).schedule(monitorOrderBookTask, 100, 1000);
                    this.cancel();
                });
    }

    private Order placeLimitOrder(SymbolInfo s, BinanceOrderBookEntry floor) {
        BigDecimal price = floor.getPrice().add(new BigDecimal(s.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize()));
        logger.info("Placing limit order for {} at price {}.", s.getSymbol(), price);
        NewOrderResponse newOrderResponse = binance.buyLimit(s.getSymbol(), BTC_PER_TRADE, price);
        return binance.getOrder(symbol, newOrderResponse.getOrderId());
    }
}

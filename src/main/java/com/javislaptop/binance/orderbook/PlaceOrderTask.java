package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.domain.OcoOrder;
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
    private static final BigDecimal STOP_LOSS_DISTANCE = new BigDecimal("0.95");
    private static final BigDecimal MIN_GAP = new BigDecimal("0.005");
    private static final BigDecimal MAX_GAP = new BigDecimal("0.05");

    private final Binance binance;
    private final SymbolInfo symbolInfo;
    private final StreamDataStorage storage;
    private final BinanceDataStreamer streamer;

    public PlaceOrderTask(Binance binance, SymbolInfo symbolInfo, StreamDataStorage storage, BinanceDataStreamer streamer) {
        this.binance = binance;
        this.symbolInfo = symbolInfo;
        this.storage = storage;
        this.streamer = streamer;
        logger.debug("Starting to watch {}", symbolInfo.getSymbol());
    }

    @Override
    public void run() {
        logger.debug("Processing order book for {}", symbolInfo.getSymbol());
        BinanceOrderBook orderBook = binance.getOrderBook(symbolInfo.getSymbol(), 500);

        orderBook.getDistanceBetweenFloorAndResistance(RESISTANCE_THRESHOLD)
                .filter(d -> d.compareTo(MIN_GAP) > 0)
                .filter(d -> d.compareTo(MAX_GAP) < 0)
                .ifPresent(s -> {
                    OcoOrder order = placeOcoOrder(symbolInfo, orderBook.findFloor(RESISTANCE_THRESHOLD).get(), orderBook.findResistance(RESISTANCE_THRESHOLD).get());

                    MonitorOrderBookTask monitorOrderBookTask = new MonitorOrderBookTask(order, storage, symbolInfo, binance, streamer);
                    new Timer("monitorbook-task-"+symbolInfo.getSymbol()).schedule(monitorOrderBookTask, 100, 100);
                    this.cancel();
                });
    }

    private OcoOrder placeOcoOrder(SymbolInfo s, BinanceOrderBookEntry floor, BinanceOrderBookEntry resistance) {
        BigDecimal price = floor.getPrice().add(new BigDecimal(s.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize()));
        BigDecimal winPrice = resistance.getPrice().subtract(new BigDecimal(s.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize()));
        BigDecimal stopLossPrice = floor.getPrice().multiply(STOP_LOSS_DISTANCE);
        logger.info("Placing limit order for {} at price {}. Stop profit: {}. Stop loss: {}", s.getSymbol(), price, winPrice, stopLossPrice);
        return new OcoOrder(price, winPrice, stopLossPrice);
    }
}

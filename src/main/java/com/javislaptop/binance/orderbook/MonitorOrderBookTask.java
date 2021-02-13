package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.event.BookTickerEvent;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.domain.OcoOrder;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorOrderBookTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(MonitorOrderBookTask.class);

    private final OcoOrder ocoOrder;
    private final StreamDataStorage storage;
    private final SymbolInfo symbolInfo;
    private final Binance binance;
    private final BinanceDataStreamer streamer;

    private boolean purchased = false;


    public MonitorOrderBookTask(OcoOrder ocoOrder, StreamDataStorage storage, SymbolInfo symbolInfo, Binance binance, BinanceDataStreamer streamer) {
        this.ocoOrder = ocoOrder;
        this.storage = storage;
        this.symbolInfo = symbolInfo;
        this.binance = binance;
        this.streamer = streamer;
        streamer.enableBookTickerEvents(symbolInfo.getSymbol());
    }

    @Override
    public void run() {
        Optional<BookTickerEvent> bookTickerEvent = storage.readBookTickerEvent(symbolInfo.getSymbol());
        bookTickerEvent.ifPresent(ticker -> {
            if (!purchased) {
                if (new BigDecimal(ticker.getAskPrice()).compareTo(ocoOrder.getBuyPrice()) <= 0) {
                    logger.info("Purchased {} at {}", ticker.getSymbol(), ocoOrder.getBuyPrice());
                    purchased = true;
                }
            } else {
                if (new BigDecimal(ticker.getBidPrice()).compareTo(ocoOrder.getProfitPrice()) >= 0) {
                    logger.info("Sold {} at {}", ticker.getSymbol(), ocoOrder.getProfitPrice());
                    logger.info("BENEFIT: {}", ocoOrder.getProfitPercent());
                    this.cancel();
                } else if (new BigDecimal(ticker.getAskPrice()).compareTo(ocoOrder.getLossPrice())<=0) {
                    logger.info("Sold {} at {}", ticker.getSymbol(), ocoOrder.getLossPrice());
                    logger.info("LOSS: {}", ocoOrder.getLossPercent());
                    this.cancel();
                }
            }
        });
    }

    @Override
    public boolean cancel() {
        TimerTask placeOrderTask = new PlaceOrderTask(binance, symbolInfo, storage, streamer);
        new Timer("update-task-"+symbolInfo.getSymbol()).schedule(placeOrderTask, 100, 1000);
        return super.cancel();
    }
}

package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;

@Service
public class OrderBookTraderWatcher {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookTraderWatcher.class);

    private final Binance binance;
    private final BinanceDataStreamer streamer;
    private final StreamDataStorage storage;

    public OrderBookTraderWatcher(Binance binance, BinanceDataStreamer streamer, StreamDataStorage storage) {
        this.binance = binance;
        this.streamer = streamer;
        this.storage = storage;
    }

    public void watch(String symbolInfo) {
        TimerTask placeOrderTask = new PlaceOrderTask(binance, symbolInfo, storage, streamer);
        new Timer("update-task-"+symbolInfo).schedule(placeOrderTask, 100, 60000);
    }


}

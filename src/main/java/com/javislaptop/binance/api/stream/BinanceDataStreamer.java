package com.javislaptop.binance.api.stream;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BinanceDataStreamer {

    private static final Logger logger = LoggerFactory.getLogger(BinanceDataStreamer.class);

    private final BinanceApiWebSocketClient binanceWebsocket;
    private final AggTradeEventCallback aggTradeEventCallback;
    private final BookTickerEventCallback bookTickerEventCallback;
    private final ConcurrentHashMap<String, Closeable> activeAggTradeEvents;
    private final ConcurrentHashMap<String, Closeable> activeBookTickerEvents;

    public BinanceDataStreamer(BinanceApiWebSocketClient binanceWebsocket, AggTradeEventCallback aggTradeEventCallback, BookTickerEventCallback bookTickerEventCallback) {
        this.binanceWebsocket = binanceWebsocket;
        this.aggTradeEventCallback = aggTradeEventCallback;
        this.bookTickerEventCallback = bookTickerEventCallback;
        this.activeAggTradeEvents = new ConcurrentHashMap<>();
        this.activeBookTickerEvents = new ConcurrentHashMap<>();
    }

    public void disableAggTradeEvents(String symbol) {
        logger.debug("Disabling agg trade events for {}", symbol);
        Closeable element = activeAggTradeEvents.remove(symbol);
        if (element != null) {
            try {
                element.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void enableAggTradeEvents(String symbol) {
        logger.debug("Enabling agg trade events for {}", symbol);
        Closeable closeable = activeAggTradeEvents.get(symbol);
        if (closeable == null) {
            enableAggTrades(symbol);
        }
    }

    public void enableBookTickerEvents(String symbol) {
        logger.debug("Enabling book events for {}", symbol);
        Closeable closeable = activeBookTickerEvents.get(symbol);
        if (closeable == null) {
            enableBookTicker(symbol);
        }
    }

    public void disableBookTickerEvents(String symbol) {
        logger.debug("Disabling book events for {}", symbol);
        Closeable element = activeBookTickerEvents.remove(symbol);
        if (element != null) {
            try {
                element.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableBookTicker(String symbol) {
        activeBookTickerEvents.put(symbol, binanceWebsocket.onDepthEvent(symbol.toLowerCase(), bookTickerEventCallback));
    }

    private void enableAggTrades(String symbol) {
        activeAggTradeEvents.put(symbol, binanceWebsocket.onAggTradeEvent(symbol.toLowerCase(), aggTradeEventCallback));
    }
}

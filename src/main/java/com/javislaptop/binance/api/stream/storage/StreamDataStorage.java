package com.javislaptop.binance.api.stream.storage;

import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.OrderBook;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBookEntry;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StreamDataStorage {

    private static final Logger logger = LoggerFactory.getLogger(StreamDataStorage.class);

    private final ConcurrentHashMap<String, List<AggTrade>> aggTrades;
    private final ConcurrentHashMap<String, DepthEvent> bookTickers;

    public StreamDataStorage() {
        aggTrades = new ConcurrentHashMap<>();
        bookTickers = new ConcurrentHashMap<>();
    }

    public void add(AggTradeEvent event) {
        logger.debug("Storing event {}", event);
        aggTrades.computeIfAbsent(event.getSymbol(), k -> Collections.synchronizedList(new ArrayList<>())).add(event);
    }

    public void add(DepthEvent event) {
        logger.debug("Storing event {}", event);
        bookTickers.put(event.getSymbol(), event);
    }

    public List<AggTrade> read(String symbol) {
        logger.debug("Reading agg trade event for {}", symbol);
        List<AggTrade> t = this.aggTrades.get(symbol);
        if (CollectionUtils.isEmpty(t)) {
            return Collections.emptyList();
        }
        List<AggTrade> trades = new ArrayList<>(t);
        this.aggTrades.put(symbol, Collections.synchronizedList(new ArrayList<>()));
        return trades;
    }

    public Optional<BinanceOrderBook> getDepth(String symbol) {
        logger.debug("Reading book ticker event for {}", symbol);
        DepthEvent t = bookTickers.get(symbol);
        if (t == null) {
            return Optional.empty();
        }
        return Optional.of(new BinanceOrderBook(
                t.getBids().parallelStream().map(bid -> new BinanceOrderBookEntry(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()))).collect(Collectors.toList()),
                t.getAsks().parallelStream().map(bid -> new BinanceOrderBookEntry(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()))).collect(Collectors.toList()),
                Instant.ofEpochMilli(t.getEventTime()),
                t.getFinalUpdateId()
                )
        );
    }
}

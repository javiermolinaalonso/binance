package com.javislaptop.binance.api.stream.storage;

import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.binance.api.client.domain.market.AggTrade;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamDataStorage {

    private static final Logger logger = LoggerFactory.getLogger(StreamDataStorage.class);

    private final ConcurrentHashMap<String, List<AggTrade>> aggTrades;
    private final ConcurrentHashMap<String, List<BookTickerEvent>> bookTickers;

    public StreamDataStorage() {
        aggTrades = new ConcurrentHashMap<>();
        bookTickers = new ConcurrentHashMap<>();
    }

    public void add(AggTradeEvent event) {
        logger.debug("Storing event {}", event);
        aggTrades.computeIfAbsent(event.getSymbol(), k -> Collections.synchronizedList(new ArrayList<>())).add(event);
    }

    public void add(BookTickerEvent event) {
        logger.debug("Storing event {}", event);
        bookTickers.computeIfAbsent(event.getSymbol(), k -> Collections.synchronizedList(new ArrayList<>())).add(event);
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

    public Optional<BookTickerEvent> readBookTickerEvent(String symbol) {
        logger.debug("Reading book ticker event for {}", symbol);
        List<BookTickerEvent> t = bookTickers.get(symbol);
        if (CollectionUtils.isEmpty(t)) {
            return Optional.empty();
        }
        List<BookTickerEvent> events = new ArrayList<>(t);
        bookTickers.put(symbol, Collections.synchronizedList(new ArrayList<>()));
        return events.stream().max(Comparator.comparingLong(BookTickerEvent::getUpdateId));
    }
}

package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.javislaptop.binance.detector.PumpData.extractVolume;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

@Service
public class OrderBookStrategy {

    private final Binance binance;
    private final BinanceApiWebSocketClient webSocketClient;
    private final PurchaseAndSellQuickStrategyProperties properties;

    public OrderBookStrategy(Binance binance, BinanceApiWebSocketClient webSocketClient, PurchaseAndSellQuickStrategyProperties properties) {
        this.binance = binance;
        this.webSocketClient = webSocketClient;
        this.properties = properties;
    }

    public void simulate(String symbol, BigDecimal amount) {
        OrderBook orderBook = binance.getOrderBook(symbol, 100);
        Map<BigDecimal, Double> bids = orderBook.getBids().stream().collect(groupingBy(a -> new BigDecimal(a.getPrice()), summingDouble(a -> extractVolume(a.getPrice(), a.getQty()))));
        Map<BigDecimal, Double> asks = orderBook.getAsks().stream().collect(groupingBy(a -> new BigDecimal(a.getPrice()), summingDouble(a -> extractVolume(a.getPrice(), a.getQty()))));

    }

}

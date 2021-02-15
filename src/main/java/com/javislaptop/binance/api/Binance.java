package com.javislaptop.binance.api;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.*;
import com.binance.api.client.exception.BinanceApiException;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.binance.api.client.domain.account.NewOrder.*;
import static com.javislaptop.binance.Utils.calculateBenefit;
import static java.util.Optional.empty;

@Service
public class Binance {

    private static final Logger logger = LoggerFactory.getLogger(Binance.class);

    private final BinanceApiRestClient binanceApiRestClient;
    private final BinanceFormatter formatter;

    public Binance(BinanceApiRestClient binanceApiRestClient, BinanceFormatter formatter) {
        this.binanceApiRestClient = binanceApiRestClient;
        this.formatter = formatter;
    }

    public Optional<BigDecimal> getAveragePrice(String symbol) {
        List<AggTrade> aggTrades = binanceApiRestClient.getAggTrades(symbol);
        double average = aggTrades.stream()
                .map(AggTrade::getPrice)
                .mapToDouble(price -> new BigDecimal(price).doubleValue())
                .average()
                .orElse(0d);
        if (average > 0) {
            return Optional.of(new BigDecimal(average));
        } else {
            return empty();
        }
    }

    public Optional<Trade> getTrade(String symbol, Long orderId) {
        return binanceApiRestClient.getMyTrades(symbol).stream().filter(t -> t.getOrderId().equals(String.valueOf(orderId))).findAny();
    }

    public NewOrderResponse buyLimit(String symbol, BigDecimal amount, BigDecimal priceLimit) {
        String priceStr = formatter.formatPrice(symbol, priceLimit);
        String amountStr = formatter.formatAmount(symbol, amount.divide(priceLimit, 8, RoundingMode.DOWN));
        logger.info("Placing limit order for {}. Amount {} at price {}", symbol, amountStr, priceStr);
        return binanceApiRestClient.newOrder(limitBuy(symbol, TimeInForce.GTC, amountStr, priceStr).newOrderRespType(NewOrderResponseType.FULL));
    }

    public Order buyMarket(String symbol, BigDecimal amount) {
        String amountStr = formatter.formatPrice(symbol, amount);
        logger.info("Placing buy market order for {}. Amount {}", symbol, amountStr);
        NewOrderResponse newOrderResponse = binanceApiRestClient.newOrder(marketBuy(symbol, null).quoteOrderQty(amountStr).newOrderRespType(NewOrderResponseType.FULL));
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    public Order sellLimit(String symbol, String amount, BigDecimal targetPrice) {
        String price = formatter.formatPrice(symbol, targetPrice);
        logger.info("Placing sell limit order for {}. Amount {} at target price {}", symbol, amount, targetPrice);
        NewOrderResponse newOrderResponse = binanceApiRestClient.newOrder(limitSell(symbol, TimeInForce.GTC, amount, price));
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    public Order sellMarket(String symbol, BigDecimal amount) {
        System.out.println(String.format("Selling %s at market", symbol));
        NewOrderResponse newOrderResponse =  binanceApiRestClient.newOrder(marketSell(symbol, formatter.formatAmount(symbol, amount)).newOrderRespType(NewOrderResponseType.FULL));
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    public List<Order> getOpenOrders(String symbol) {
        return binanceApiRestClient.getOpenOrders(new OrderRequest(symbol));
    }

    public Order getOrder(String symbol, Long orderId) {
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(symbol, orderId);
        int count = 0;
        Order order = null;
        do {
            try {
                count += 100;
                order = binanceApiRestClient.getOrderStatus(orderStatusRequest);
            } catch (BinanceApiException e) {
                System.out.println(e.getMessage());
            }
            if (order == null || order.getStatus() != OrderStatus.FILLED) {
                try {
                    System.out.println("Market limit order not filled");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while ((order == null || order.getStatus() != OrderStatus.FILLED) && count < 2000);

        return order;
    }

    public CancelOrderResponse cancelOrder(String symbol, Long orderId) {
        logger.info("Cancelling order for {} with id {}", symbol, orderId);
        return binanceApiRestClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
    }

    public String getAssetBalance(String asset) {
        return binanceApiRestClient.getAccount().getAssetBalance(asset).getFree();
    }

    public void printPrices(String symbol, String purchasePricestr) {
        BigDecimal price = getBuyPrice(symbol);
        BigDecimal purchasePrice = new BigDecimal(purchasePricestr);
        BigDecimal benefit = calculateBenefit(price, purchasePrice);
        System.out.println("Expected benefit: " + benefit.toPlainString() + "%");
    }

    public ExchangeInfo getExchangeInfo() {
        return binanceApiRestClient.getExchangeInfo();
    }

    public SymbolInfo getSymbolInfo(String symbol) {
        return binanceApiRestClient.getExchangeInfo()
                .getSymbolInfo(symbol);
    }

    public BinanceOrderBook getOrderBook(String symbol, int limit) {
        OrderBook orderBook = binanceApiRestClient.getOrderBook(symbol, limit);
        return new BinanceOrderBook(
                orderBook.getBids().parallelStream().map(bid -> new BinanceOrderBookEntry(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()))).collect(Collectors.toList()),
                orderBook.getAsks().parallelStream().map(bid -> new BinanceOrderBookEntry(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()))).collect(Collectors.toList()),
                Instant.now(Clock.systemUTC()),
                orderBook.getLastUpdateId()
        );
    }

    public BigDecimal getBuyPrice(String symbol) {
        String price = binanceApiRestClient.getOrderBook(symbol, 5)
                .getAsks()
                .stream()
                .sorted(Comparator.comparing(OrderBookEntry::getPrice))
                .map(OrderBookEntry::getPrice)
                .findFirst()
                .orElseGet(
                        () -> binanceApiRestClient.getPrice(symbol).getPrice()
                );
        return new BigDecimal(price);
    }

    public BigDecimal getSellPrice(String symbol) {
        String price = binanceApiRestClient.getOrderBook(symbol, 5)
                .getBids()
                .stream()
                .sorted((a, b) -> b.getPrice().compareTo(a.getPrice()))
                .map(OrderBookEntry::getPrice)
                .findFirst()
                .orElseGet(
                        () -> binanceApiRestClient.getPrice(symbol).getPrice()
                );
        return new BigDecimal(price);
    }

    public List<Candlestick> getLastHour(String symbol, Instant when) {
        Instant from = when.minus(65, ChronoUnit.MINUTES);
        Instant to = when.minus(5, ChronoUnit.MINUTES);
        return binanceApiRestClient.getCandlestickBars(symbol, CandlestickInterval.ONE_MINUTE, 60, from.toEpochMilli(), to.toEpochMilli());
    }
}

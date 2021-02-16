package com.javislaptop.binance.api;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.*;
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

    private static final Long REC_WINDOW = 2000L;

    private final BinanceApiRestClient binanceApiRestClient;
    private final BinanceFormatter formatter;
    private final Clock clock;

    public Binance(BinanceApiRestClient binanceApiRestClient, BinanceFormatter formatter, Clock clock) {
        this.binanceApiRestClient = binanceApiRestClient;
        this.formatter = formatter;
        this.clock = clock;
        long systemTime = Instant.now(clock).toEpochMilli();
        Long serverTime = binanceApiRestClient.getServerTime();
        System.out.println(serverTime);
        System.out.println(systemTime);
        System.out.println("Adjusting difference time of " + String.valueOf(systemTime - serverTime));

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
        NewOrder buyOrder = limitBuy(symbol, TimeInForce.GTC, amountStr, priceStr).newOrderRespType(NewOrderResponseType.FULL).timestamp(Instant.now(clock).toEpochMilli());
        buyOrder.recvWindow(REC_WINDOW);
        return binanceApiRestClient.newOrder(buyOrder);
    }

    public Order buyMarket(String symbol, BigDecimal amount) {
        String amountStr = formatter.formatPrice(symbol, amount);
        logger.info("Placing buy market order for {}. Amount {}", symbol, amountStr);
        NewOrder buyOrder = marketBuy(symbol, null).quoteOrderQty(amountStr).newOrderRespType(NewOrderResponseType.FULL);
        buyOrder.recvWindow(REC_WINDOW);
        NewOrderResponse newOrderResponse = binanceApiRestClient.newOrder(buyOrder);
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    public NewOrderResponse sellLimit(String symbol, String amount, BigDecimal targetPrice) {
        String price = formatter.formatPrice(symbol, targetPrice);
        logger.info("Placing sell limit order for {}. Amount {} at target price {}", symbol, amount, targetPrice);
        NewOrder order = limitSell(symbol, TimeInForce.GTC, amount, price);
        order.recvWindow(REC_WINDOW);
        return binanceApiRestClient.newOrder(order);
    }

    public Order sellMarket(String symbol, BigDecimal amount) {
        System.out.println(String.format("Selling %s at market", symbol));
        NewOrderResponse newOrderResponse =  binanceApiRestClient.newOrder(marketSell(symbol, formatter.formatAmount(symbol, amount)).newOrderRespType(NewOrderResponseType.FULL));
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    public List<Order> getOpenOrders(String symbol) {
        OrderRequest order = new OrderRequest(symbol).timestamp(Instant.now(clock).toEpochMilli());
        order.recvWindow(REC_WINDOW);
        return binanceApiRestClient.getOpenOrders(order);
    }

    public Order getOrder(String symbol, Long orderId) {
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(symbol, orderId);
        orderStatusRequest.recvWindow(REC_WINDOW);
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

    public CancelOrderResponse cancelOrder(String symbol, Long orderId, String orderReqId) {
        logger.info("Cancelling order for {} with id {}", symbol, orderId);
        try {
            CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(symbol, orderId);
            cancelOrderRequest.timestamp(Instant.now(clock).toEpochMilli());
            cancelOrderRequest.recvWindow(REC_WINDOW);
            return binanceApiRestClient.cancelOrder(cancelOrderRequest);
        } catch (BinanceApiException e) {
            logger.error("There was an error when cancelling the request for {} with id {}", symbol, orderId);
            return null;
        }
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
                Instant.now(clock),
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

    public List<Order> getAccountOrders(String symbol) {
        AllOrdersRequest orderRequest = new AllOrdersRequest(symbol);
        orderRequest.recvWindow(REC_WINDOW);
        return binanceApiRestClient.getAllOrders(orderRequest);
    }

    public CancelOrderResponse cancelOrder(Order order) {
        return cancelOrder(order.getSymbol(), order.getOrderId(), order.getClientOrderId());
    }
}

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
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.*;
import com.binance.api.client.exception.BinanceApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.binance.api.client.domain.account.NewOrder.*;
import static java.util.Optional.empty;

@Service
public class Binance {

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
    public Order buyLimit(String symbol, BigDecimal amount, BigDecimal priceLimit) {
        String priceStr = formatter.formatPrice(symbol, priceLimit);
        String amountStr = formatter.formatAmount(symbol, amount.divide(priceLimit, 8, RoundingMode.DOWN));
        System.out.println(String.format("Placing limit order for %s. Amount %s at price %s", symbol, amountStr, priceStr));
        Long orderId = binanceApiRestClient.newOrder(limitBuy(symbol, TimeInForce.GTC, amountStr, priceStr).newOrderRespType(NewOrderResponseType.FULL)).getOrderId();

        return getOrder(symbol, orderId);
    }

    public Order buyMarket(String symbol, BigDecimal amount) {
        String amountStr = formatter.formatPrice(symbol, amount);
        NewOrderResponse newOrderResponse = binanceApiRestClient.newOrder(marketBuy(symbol, null).quoteOrderQty(amountStr).newOrderRespType(NewOrderResponseType.FULL));
        return getOrder(symbol, newOrderResponse.getOrderId());
    }

    private Order getOrder(String symbol, Long orderId) {
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(symbol, orderId);
        int count = 0;
        Order order = null;
        do {
            try {
                System.out.println("Market limit order not filled");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                count += 100;
                order = binanceApiRestClient.getOrderStatus(orderStatusRequest);
            } catch (BinanceApiException e) {
                System.out.println(e.getMessage());
            }

        } while ((order == null || order.getStatus() != OrderStatus.FILLED) && count < 2000);

        return order;
    }

    public NewOrderResponse sellLimit(String symbol, String amount, BigDecimal targetPrice) {
        String price = formatter.formatPrice(symbol, targetPrice);
        System.out.println("Selling at target price " + targetPrice);
        return binanceApiRestClient.newOrder(limitSell(symbol, TimeInForce.GTC, amount, price));
    }

    public NewOrderResponse sellMarket(String symbol, String amount) {
        return binanceApiRestClient.newOrder(marketSell(symbol, amount).newOrderRespType(NewOrderResponseType.FULL));
    }

    public CancelOrderResponse cancelOrder(String symbol, Long orderId) {
        return binanceApiRestClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
    }

    public String getAssetBalance(String asset) {
        return binanceApiRestClient.getAccount().getAssetBalance(asset).getFree();
    }

    public void printPrices(String symbol, String purchasePricestr) {
        String p = binanceApiRestClient.getOrderBook(symbol, 5).getBids().stream().map(OrderBookEntry::getPrice).findAny().orElseGet(
                () -> binanceApiRestClient.getPrice(symbol).getPrice()
        );
        BigDecimal price = new BigDecimal(p);
        BigDecimal purchasePrice = new BigDecimal(purchasePricestr);
        BigDecimal benefit = price.subtract(purchasePrice).divide(purchasePrice, 8, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));
        System.out.println("Expected benefit: " + benefit.toPlainString() + "%");
    }

    public ExchangeInfo getExchangeInfo() {
        return binanceApiRestClient.getExchangeInfo();
    }

    public List<Candlestick> getLastHour(String symbol) {
        Instant from = Instant.now(Clock.systemUTC()).minus(65, ChronoUnit.MINUTES);
        Instant to = Instant.now(Clock.systemUTC()).minus(5, ChronoUnit.MINUTES);
        return binanceApiRestClient.getCandlestickBars(symbol, CandlestickInterval.ONE_MINUTE, 60, from.toEpochMilli(), to.toEpochMilli());
    }

    public OrderBook getOrderBook(String symbol) {
        return binanceApiRestClient.getOrderBook(symbol, 5000);
    }
}

package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.javislaptop.binance.api.Binance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.javislaptop.binance.Utils.calculateBenefit;

@Service
@EnableConfigurationProperties(PurchaseAndSellQuickStrategyProperties.class)
public class PurchaseAndSellQuickStrategy {

    private final Binance binance;
    private final BinanceApiWebSocketClient webSocketClient;
    private final PurchaseAndSellQuickStrategyProperties properties;

    public PurchaseAndSellQuickStrategy(Binance binance, BinanceApiWebSocketClient webSocketClient, PurchaseAndSellQuickStrategyProperties properties) {
        this.binance = binance;
        this.webSocketClient = webSocketClient;
        this.properties = properties;
    }

    public void simulate(String symbol, BigDecimal amount) {
        BigDecimal buyPrice = binance.getBuyPrice(symbol);
        System.out.println(String.format("[%s] Buy %s at %s\n", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, buyPrice));
        AtomicBoolean exitTrade = new AtomicBoolean(false);

        BinanceApiCallback<BookTickerEvent> callback = new BinanceApiCallback<BookTickerEvent>() {
            @Override
            public void onResponse(BookTickerEvent response) {
                if (exitTrade.get()) {
                    return;
                }
                BigDecimal bidPrice = new BigDecimal(response.getBidPrice());
                BigDecimal benefitPercent = calculateBenefit(buyPrice, bidPrice);

                if (benefitPercent.compareTo(new BigDecimal(properties.getBenefitPercent())) > 0) {
                    System.out.println(String.format("[%s] Sold order for %s at %s with benefit %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, bidPrice, benefitPercent));
                    exitTrade.set(true);
                } else if (benefitPercent.compareTo(new BigDecimal(properties.getLossPercent())) < 0) {
                    System.out.println(String.format("[%s] Sold order for %s at %s with loss %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, bidPrice, benefitPercent));
                    exitTrade.set(true);
                }
            }
        };
        Closeable closeable = webSocketClient.onBookTickerEvent(symbol.toLowerCase(), callback);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!exitTrade.get()) {
                    BigDecimal sellPrice = binance.getSellPrice(symbol);
                    BigDecimal benefitPercent = calculateBenefit(buyPrice, sellPrice);
                    System.out.println(String.format("[%s] Closing trade for %s at %s because of time with benefit %.4f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, sellPrice, benefitPercent));
                }
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Timer("Simulation " + symbol)
                .schedule(task, properties.getTimeout());
    }

    public void execute(String symbol, BigDecimal amount) {
        BigDecimal limit = binance.getBuyPrice(symbol);
        Order buyOrder = binance.buyLimit(symbol, amount, limit);
        if (buyOrder == null || buyOrder.getStatus() != OrderStatus.FILLED) {
            System.out.println("Order not filled, returning");
            return;
        }
        BigDecimal buyPrice = new BigDecimal(buyOrder.getPrice());
        System.out.println(String.format("[%s] Buy %s at %s\n", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, buyPrice));
        AtomicBoolean exitTrade = new AtomicBoolean(false);

        BinanceApiCallback<BookTickerEvent> callback = new BinanceApiCallback<BookTickerEvent>() {
            @Override
            public void onResponse(BookTickerEvent response) {
                if (exitTrade.get()) {
                    return;
                }
                BigDecimal bidPrice = new BigDecimal(response.getBidPrice());
                BigDecimal benefitPercent = calculateBenefit(buyPrice, bidPrice);

                if (benefitPercent.compareTo(new BigDecimal("0.499")) > 0) {
                    NewOrderResponse sellResponse = binance.sellMarket(symbol, buyOrder.getExecutedQty());
                    System.out.println(String.format("Sold order with benefit %.2f", benefitPercent));
                    exitTrade.set(true);
                } else if (benefitPercent.compareTo(new BigDecimal("-0.5")) < 0) {
                    binance.sellMarket(symbol, buyOrder.getExecutedQty());
                    System.out.println(String.format("Sold order with loss %.2f", benefitPercent));
                    exitTrade.set(true);
                } else {
                    System.out.println("Still in!");
                }
            }
        };
        Closeable closeable = webSocketClient.onBookTickerEvent(symbol, callback);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!exitTrade.get()) {
                    NewOrderResponse sellResponse = binance.sellMarket(symbol, buyOrder.getExecutedQty());
                    BigDecimal sellPrice = binance.getSellPrice(symbol);
                    BigDecimal benefitPercent = calculateBenefit(buyPrice, sellPrice);
                    System.out.println(String.format("Closing trade because of time with benefit %.4f", benefitPercent));
                }
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Timer("Execution " + symbol)
                .schedule(task, 5000);

    }
}

package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.BookTickerEvent;

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

public abstract class AbstractPurchaseAndSellQuickStrategy implements TradeStrategy {

    private final BinanceApiWebSocketClient webSocketClient;
    final PurchaseAndSellQuickStrategyProperties properties;
    final AtomicBoolean exitTrade;
    BigDecimal buyPrice;
    BigDecimal sellPrice;

    public AbstractPurchaseAndSellQuickStrategy(BinanceApiWebSocketClient webSocketClient, PurchaseAndSellQuickStrategyProperties properties) {
        this.webSocketClient = webSocketClient;
        this.properties = properties;
        this.exitTrade = new AtomicBoolean(false);
    }

    public void execute(String symbol) {
        purchase(symbol);
        System.out.println(String.format("[%s] Buy %s at %s\n", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, buyPrice));
        
        Closeable closeable = webSocketClient.onBookTickerEvent(symbol.toLowerCase(), getCallback(symbol));

        TimerTask exitTimeTask = getTimeExitTask(symbol, closeable);
        new Timer(symbol).schedule(exitTimeTask, properties.getTimeout());
    }

    protected abstract void purchase(String symbol);

    protected BinanceApiCallback<BookTickerEvent> getCallback(String symbol) {
        return response -> {
            if (exitTrade.get()) {
                return;
            }
            BigDecimal bidPrice = new BigDecimal(response.getBidPrice());
            BigDecimal benefitPercent = calculateBenefit(buyPrice, bidPrice);

            if (benefitPercent.compareTo(new BigDecimal(properties.getBenefitPercent())) > 0) {
                sell(symbol, bidPrice);
                System.out.println(String.format("[%s] Sold order for %s at %s with benefit %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, bidPrice, benefitPercent));
                exitTrade.set(true);
            } else if (benefitPercent.compareTo(new BigDecimal(properties.getLossPercent())) < 0) {
                sell(symbol, bidPrice);
                System.out.println(String.format("[%s] Sold order for %s at %s with loss %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, bidPrice, benefitPercent));
                exitTrade.set(true);
            }
        };
    }

    
    
    private TimerTask getTimeExitTask(String symbol, Closeable closeable) {
        return new TimerTask() {
            @Override
            public void run() {
                if (!exitTrade.get()) {
                    sell(symbol);
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
    }

    protected abstract void sell(String symbol, BigDecimal bidPrice);

    protected abstract void sell(String symbol);
}

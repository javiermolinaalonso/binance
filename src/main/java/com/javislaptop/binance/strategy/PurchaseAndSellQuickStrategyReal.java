package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.javislaptop.binance.api.Binance;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;


@Service
@EnableConfigurationProperties(PurchaseAndSellQuickStrategyProperties.class)
@Profile("real")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PurchaseAndSellQuickStrategyReal extends AbstractPurchaseAndSellQuickStrategy {

    private final Binance binance;

    private Order buyOrder;
    public PurchaseAndSellQuickStrategyReal(Binance binance, BinanceApiWebSocketClient webSocketClient, PurchaseAndSellQuickStrategyProperties properties) {
        super(webSocketClient, properties);
        this.binance = binance;
    }

    @Override
    protected void purchase(String symbol) {
        buyPrice = binance.getBuyPrice(symbol);
        buyOrder = binance.buyLimit(symbol, new BigDecimal(properties.getPurchaseAmount()), buyPrice);
    }

    @Override
    protected void sell(String symbol, BigDecimal bidPrice) {
        Order order = binance.sellLimit(symbol, buyOrder.getExecutedQty(), bidPrice);
        sellPrice = new BigDecimal(order.getPrice());
        sellToMarket(symbol, order);
    }

    @Override
    protected void sell(String symbol) {
        BigDecimal sellPrice = binance.getSellPrice(symbol);
        Order order = binance.sellLimit(symbol, buyOrder.getExecutedQty(), sellPrice);
        this.sellPrice = new BigDecimal(order.getPrice());
        sellToMarket(symbol, order);
    }

    private void sellToMarket(String symbol, Order order) {
        if (order.getStatus() != OrderStatus.FILLED) {
            binance.cancelOrder(symbol, order.getOrderId());
            binance.sellMarket(symbol, new BigDecimal(buyOrder.getExecutedQty()));
        }
    }
}

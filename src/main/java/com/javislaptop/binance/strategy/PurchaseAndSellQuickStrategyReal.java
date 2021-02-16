package com.javislaptop.binance.strategy;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
@EnableConfigurationProperties(PurchaseAndSellQuickStrategyProperties.class)
@Profile("real")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PurchaseAndSellQuickStrategyReal extends AbstractPurchaseAndSellQuickStrategy {

    private static final Logger logger = LogManager.getLogger(PurchaseAndSellQuickStrategyReal.class);

    private final Binance binance;
    private Order buyOrder;

    public PurchaseAndSellQuickStrategyReal(Binance binance, BinanceDataStreamer dataStreamer, PurchaseAndSellQuickStrategyProperties properties, StreamDataStorage storage) {
        super(dataStreamer, storage, properties);
        this.binance = binance;
    }

    @Override
    protected void purchase(String symbol) {
        buyPrice = binance.getBuyPrice(symbol);
        NewOrderResponse newOrderResponse = binance.buyLimit(symbol, new BigDecimal(properties.getPurchaseAmount()), buyPrice);
        buyOrder = binance.getOrder(symbol, newOrderResponse.getOrderId());
        logger.info(String.format("[%s] Buy %s at %s", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, buyPrice));
        if (buyOrder.getStatus() == OrderStatus.FILLED) {
            BigDecimal sellLimit = new BigDecimal(buyOrder.getPrice()).multiply(new BigDecimal(properties.getBenefitPercent()));
            binance.sellLimit(symbol, buyOrder.getExecutedQty(), sellLimit);
        }
    }

    @Override
    protected void sell(String symbol, BigDecimal bidPrice) {
        NewOrderResponse order = binance.sellLimit(symbol, buyOrder.getExecutedQty(), bidPrice);
        sellPrice = new BigDecimal(order.getPrice());
        sellToMarket(symbol, order);
    }

    @Override
    protected void sell(String symbol) {
        BigDecimal sellPrice = binance.getSellPrice(symbol);
        NewOrderResponse order = binance.sellLimit(symbol, buyOrder.getExecutedQty(), sellPrice);
        this.sellPrice = new BigDecimal(order.getPrice());
        sellToMarket(symbol, order);
    }

    private void sellToMarket(String symbol, NewOrderResponse order) {
        if (order.getStatus() != OrderStatus.FILLED) {
            binance.cancelOrder(symbol, order.getOrderId(), null);
            binance.sellMarket(symbol, new BigDecimal(buyOrder.getExecutedQty()));
        }
    }
}

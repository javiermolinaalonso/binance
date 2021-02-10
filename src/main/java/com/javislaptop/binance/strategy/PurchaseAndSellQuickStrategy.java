package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiWebSocketClient;
import com.javislaptop.binance.api.Binance;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@EnableConfigurationProperties(PurchaseAndSellQuickStrategyProperties.class)
@Profile("test")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PurchaseAndSellQuickStrategy extends AbstractPurchaseAndSellQuickStrategy {

    private final Binance binance;
    private final PurchaseAndSellQuickStrategyProperties properties;


    public PurchaseAndSellQuickStrategy(Binance binance, BinanceApiWebSocketClient webSocketClient, PurchaseAndSellQuickStrategyProperties properties) {
        super(webSocketClient, properties);
        this.binance = binance;
        this.properties = properties;
    }

    @Override
    protected void purchase(String symbol) {
        binance.testBuyMarket(symbol, new BigDecimal(properties.getPurchaseAmount()));
        buyPrice = binance.getBuyPrice(symbol);
    }

    @Override
    protected void sell(String symbol, BigDecimal bidPrice) {
        binance.testSellLimit(symbol, bidPrice);
    }

    @Override
    protected void sell(String symbol) {
        binance.testSellMarket(symbol);
    }
}

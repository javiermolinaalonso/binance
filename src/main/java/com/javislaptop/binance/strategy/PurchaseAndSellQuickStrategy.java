package com.javislaptop.binance.strategy;

import com.binance.api.client.BinanceApiWebSocketClient;
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
@Profile("test")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PurchaseAndSellQuickStrategy extends AbstractPurchaseAndSellQuickStrategy {

    private static final Logger logger = LogManager.getLogger(PurchaseAndSellQuickStrategy.class);

    private final Binance binance;

    public PurchaseAndSellQuickStrategy(Binance binance, BinanceDataStreamer binanceDataStreamer, PurchaseAndSellQuickStrategyProperties properties, StreamDataStorage storage) {
        super(binanceDataStreamer, storage, properties);
        this.binance = binance;
    }

    @Override
    protected void purchase(String symbol) {
        buyPrice = binance.getBuyPrice(symbol);
        logger.info(String.format("[%s] Buy %s at %s", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, buyPrice));
    }

    @Override
    protected void sell(String symbol, BigDecimal bidPrice) {
        sellPrice = binance.getSellPrice(symbol);
    }

    @Override
    protected void sell(String symbol) {
        sellPrice = binance.getSellPrice(symbol);
    }
}

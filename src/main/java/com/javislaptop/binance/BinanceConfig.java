package com.javislaptop.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceConfig {

    private final BinanceProperties binanceProperties;

    public BinanceConfig(BinanceProperties binanceProperties) {
        this.binanceProperties = binanceProperties;
    }

    @Bean
    public BinanceApiRestClient binanceApiRestClient() {
        return BinanceApiClientFactory
                .newInstance(binanceProperties.getKey(), binanceProperties.getSecret())
                .newRestClient();
    }
}

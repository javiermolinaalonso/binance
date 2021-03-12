package com.javislaptop.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Scanner;

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

    @Bean
    public BinanceApiWebSocketClient binanceApiWebSocketClient() {
        return BinanceApiClientFactory
                .newInstance(binanceProperties.getKey(), binanceProperties.getSecret())
                .newWebSocketClient();
    }

    @Bean
    public Scanner scanner() {
        return new Scanner(System.in);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }
}

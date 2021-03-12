package com.javislaptop.binance.detector.martingala;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.javislaptop.binance.api.Binance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication(scanBasePackageClasses = {MartingalaRealtime.class, Binance.class})
//@EnableConfigurationProperties(BinanceProperties.class)
@EnableScheduling
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MartingalaApplication.class, args);
    }

    @Bean
    public BinanceApiRestClient binanceApiRestClient() {
        return BinanceApiClientFactory
                .newInstance()
                .newRestClient();
    }

    @Bean
    public BinanceApiWebSocketClient binanceApiWebSocketClient() {
        return BinanceApiClientFactory
                .newInstance()
                .newWebSocketClient();
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }
}

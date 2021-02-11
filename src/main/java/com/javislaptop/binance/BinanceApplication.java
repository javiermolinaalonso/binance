package com.javislaptop.binance;

import com.javislaptop.binance.detector.RealtimePumpDetector;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceApplication.class, args);
    }

    private final RealtimePumpDetector realtimePumpDetector;

    public BinanceApplication(RealtimePumpDetector realtimePumpDetector) {
        this.realtimePumpDetector = realtimePumpDetector;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            realtimePumpDetector.enablePumpDetection();
        };
    }

}

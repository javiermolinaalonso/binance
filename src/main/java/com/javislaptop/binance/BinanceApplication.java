package com.javislaptop.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceApplication.class, args);
    }

    private final BinanceApiRestClient binance;

    public BinanceApplication(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("1. pump");
            System.out.println("2. quit");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                new PumpNDumper(binance, scanner).execute();
            }


            System.exit(0);
        };
    }

}

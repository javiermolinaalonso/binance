package com.javislaptop.binance;

import com.javislaptop.binance.detector.arbitrage.ArbitrageService;
import com.javislaptop.binance.detector.pump.RealtimePumpDetector;
import com.javislaptop.binance.orderbook.OrderBookTrader;
import com.javislaptop.binance.pumper.PumpNDumper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
@EnableScheduling
public class BinanceApplication {



    public static void main(String[] args) {
        SpringApplication.run(BinanceApplication.class, args);
    }

    private final Scanner scanner;
    private final PumpNDumper pumpNDumper;
    private final RealtimePumpDetector realtimePumpDetector;
    private final ArbitrageService arbitrageService;
    private final OrderBookTrader orderBookTrader;

    public BinanceApplication(Scanner scanner, PumpNDumper pumpNDumper, RealtimePumpDetector realtimePumpDetector, ArbitrageService arbitrageService, OrderBookTrader orderBookTrader) {
        this.scanner = scanner;
        this.pumpNDumper = pumpNDumper;
        this.realtimePumpDetector = realtimePumpDetector;
        this.arbitrageService = arbitrageService;
        this.orderBookTrader = orderBookTrader;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            try {
            System.out.println("1. Manual pump");
            System.out.println("2. Automated pump");
            System.out.println("3. Arbitrage");
            System.out.println("4. Order Book trading");
            System.out.println("9. quit");
//            String option = scanner.nextLine();
                String option = "3";
            if (option.equals("1")) {
                pumpNDumper.execute();
            } else if (option.equals("2")) {
                realtimePumpDetector.enablePumpDetection();
            } else if(option.equals("3")) {
                arbitrageService.execute();
            } else if (option.equals("4")) {
                orderBookTrader.execute();
            }
            } catch (Exception e ){
                e.printStackTrace();
            }
        };
    }

}

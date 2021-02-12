package com.javislaptop.binance;

import com.javislaptop.binance.detector.coinpair.CoinPairAnalyzer;
import com.javislaptop.binance.detector.pump.HistoricalPumpDetector;
import com.javislaptop.binance.detector.pump.RealtimePumpDetector;
import com.javislaptop.binance.pumper.PumpNDumper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceApplication.class, args);
    }

    private final Scanner scanner;
    private final PumpNDumper pumpNDumper;
    private final RealtimePumpDetector realtimePumpDetector;
    private final CoinPairAnalyzer coinPairAnalyzer;

    public BinanceApplication(Scanner scanner, PumpNDumper pumpNDumper, RealtimePumpDetector realtimePumpDetector, CoinPairAnalyzer coinPairAnalyzer) {
        this.scanner = scanner;
        this.pumpNDumper = pumpNDumper;
        this.realtimePumpDetector = realtimePumpDetector;
        this.coinPairAnalyzer = coinPairAnalyzer;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            try {
            System.out.println("1. Manual pump");
            System.out.println("2. Automated pump");
            System.out.println("3. Analyze pairs");
            System.out.println("9. quit");
//            String option = scanner.nextLine();
                String option = "3";
            if (option.equals("1")) {
                pumpNDumper.execute();
            } else if (option.equals("2")) {
                realtimePumpDetector.enablePumpDetection();
            } else if(option.equals("3")) {
                coinPairAnalyzer.execute();
            }
            } catch (Exception e ){
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        };
    }

}

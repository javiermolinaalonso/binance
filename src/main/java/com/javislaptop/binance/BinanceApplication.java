package com.javislaptop.binance;

import com.javislaptop.binance.detector.HistoricalPumpDetector;
import com.javislaptop.binance.detector.RealtimePumpDetector;
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
    private final HistoricalPumpDetector historicalPumpDetector;

    public BinanceApplication(Scanner scanner, PumpNDumper pumpNDumper, RealtimePumpDetector realtimePumpDetector, HistoricalPumpDetector historicalPumpDetector) {
        this.scanner = scanner;
        this.pumpNDumper = pumpNDumper;
        this.realtimePumpDetector = realtimePumpDetector;
        this.historicalPumpDetector = historicalPumpDetector;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            try {
            System.out.println("1. Manual pump");
            System.out.println("2. Automated pump");
            System.out.println("9. quit");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                pumpNDumper.execute();
            } else if (option.equals("2")) {
                realtimePumpDetector.enablePumpDetection();
            }
            } catch (Exception e ){
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        };
    }

}

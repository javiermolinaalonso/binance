package com.javislaptop.binance;

import com.javislaptop.binance.detector.PumpDetector;
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
    private final PumpDetector pumpDetector;
    private final PumpNDumper pumpNDumper;

    public BinanceApplication(Scanner scanner, PumpDetector pumpDetector, PumpNDumper pumpNDumper) {
        this.scanner = scanner;
        this.pumpDetector = pumpDetector;
        this.pumpNDumper = pumpNDumper;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            try {
//            System.out.println("1. pump");
//            System.out.println("2. detect");
//            System.out.println("9. quit");
//            String option = scanner.nextLine();
//            if (option.equals("1")) {
                pumpNDumper.execute();
//            } else if (option.equals("2")) {
//                pumpDetector.showPumps();
//            }
            } catch (Exception e ){
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        };
    }

}

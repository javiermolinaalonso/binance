package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.domain.Candlestick;
import com.javislaptop.binance.detector.pump.PumpInstantDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static java.math.BigDecimal.valueOf;
import static java.util.stream.Collectors.joining;

@Service
public class MartingalaDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);
    public static final BigDecimal PERCENTAGE_THRESHOLD = new BigDecimal("0.99");

    private final Binance binance;

    public MartingalaDetector(Binance binance) {
        this.binance = binance;
    }

    public void execute() {
        String symbol = "NANOBTC";


        Instant from = LocalDate.of(2021, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = LocalDate.of(2021, 2, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant currentTo = from.plus(1, ChronoUnit.DAYS);

        List<BigDecimal> purchases = new ArrayList<>();
        List<BigDecimal> benefits = new ArrayList<>();
        while (from.compareTo(to) < 0) {
            List<Candlestick> candlesticks = binance.getMinuteBar(symbol, from, currentTo);

            BigDecimal referencePrice = candlesticks.get(0).getOpen();
            BigDecimal sellPrice = candlesticks.get(candlesticks.size() - 1).getClose();
            BigDecimal currentThreshold = referencePrice.multiply(PERCENTAGE_THRESHOLD);
            int count = 0;
            for (Candlestick c : candlesticks) {
                if (c.getClose().compareTo(currentThreshold) < 0) {
                    count++;
                    purchases.add(currentThreshold);
                    currentThreshold = currentThreshold.multiply(PERCENTAGE_THRESHOLD);
                }
            }

            logger.debug("Purchases {}", purchases.stream().map(BigDecimal::toString).collect(joining(",", "[", "]")));
            logger.debug("Sell at {}", sellPrice);
            OptionalDouble average = purchases.stream().mapToDouble(BigDecimal::doubleValue).average();
            if (average.isPresent()) {
                BigDecimal precisionNum = valueOf(average.getAsDouble());
                BigDecimal benefit = sellPrice.divide(precisionNum, 8, RoundingMode.HALF_DOWN).subtract(BigDecimal.ONE);
                benefits.add(benefit);
                logger.info("Aprox benefit: {}. Purchases: {}", benefit, count);
            }
            from = from.plus(1, ChronoUnit.DAYS);
            currentTo = currentTo.plus(1, ChronoUnit.DAYS);
            purchases = new ArrayList<>();
        }
        double periodBenefit = benefits.stream().mapToDouble(BigDecimal::doubleValue).sum();
        logger.info("Benefit of period {}", periodBenefit);
    }

}

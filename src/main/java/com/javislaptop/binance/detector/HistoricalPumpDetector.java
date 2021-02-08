package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class HistoricalPumpDetector {

    private final BinanceApiRestClient binance;
    private final PumpInstantDetector pumpInstantDetector;

    public HistoricalPumpDetector(BinanceApiRestClient binance, PumpInstantDetector pumpInstantDetector) {
        this.binance = binance;
        this.pumpInstantDetector = pumpInstantDetector;
    }

    public void showPumps() {
        LocalDateTime from = LocalDateTime.of(2021, 2, 3, 20, 55, 0);
        LocalDateTime to = LocalDateTime.of(2021, 2, 3, 21, 10, 0);

        Duration duration = Duration.ofHours(1);
        LocalDateTime currentStart = from;
        LocalDateTime currentEnd = from.plus(duration);
        if (to.compareTo(currentEnd) < 0) {
            currentEnd = to;
        }
        String symbol = "SKYBTC";
        do {
            List<AggTrade> trades = binance.getAggTrades(symbol, null, 100000, currentStart.toInstant(ZoneOffset.UTC).toEpochMilli(), currentEnd.toInstant(ZoneOffset.UTC).toEpochMilli());

            pumpInstantDetector.detect(symbol, trades);

            currentStart = currentEnd;
            currentEnd = currentStart.plus(duration);
        } while (currentEnd.compareTo(to) <= 0);



    }
}

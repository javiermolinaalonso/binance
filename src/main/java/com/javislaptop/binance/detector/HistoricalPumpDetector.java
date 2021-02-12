package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.stream.Collectors.toMap;

@Service
public class HistoricalPumpDetector {

    private final BinanceApiRestClient binance;
    private final PumpInstantDetector pumpInstantDetector;

    public HistoricalPumpDetector(BinanceApiRestClient binance, PumpInstantDetector pumpInstantDetector) {
        this.binance = binance;
        this.pumpInstantDetector = pumpInstantDetector;
    }

    public void enablePumpDetection() {
        LocalDateTime from = LocalDateTime.of(2021, 2, 5, 20, 59, 55);
        LocalDateTime to = LocalDateTime.of(2021, 2, 5, 21, 0, 10);

        Duration duration = Duration.ofHours(1);
        LocalDateTime currentStart = from;
        LocalDateTime currentEnd = from.plus(duration);
        if (to.compareTo(currentEnd) < 0) {
            currentEnd = to;
        }
        String symbol = "VIBETH";
        do {
            System.out.println("Timestamp, qty, price, buyermaker");
            List<AggTrade> trades = binance.getAggTrades(symbol, null, 100000, currentStart.toInstant(ZoneOffset.UTC).toEpochMilli(), currentEnd.toInstant(ZoneOffset.UTC).toEpochMilli());
            long tradeTime = trades.get(0).getTradeTime();
            trades.stream()
//                    .flatMap(t -> binance.getHistoricalTrades(symbol, Long.valueOf(t.getLastBreakdownTradeId() - t.getFirstBreakdownTradeId()).intValue() + 1, t.getFirstBreakdownTradeId()).stream())
                    .forEach(t -> System.out.println(String.format("%s, %s, %s, %s", t.getTradeTime() - tradeTime, t.getQuantity(), t.getPrice(), t.isBuyerMaker())));

//            pumpInstantDetector.detect(symbol, trades);

            currentStart = currentEnd;
            currentEnd = currentStart.plus(duration);
        } while (currentEnd.compareTo(to) <= 0);


    }
}

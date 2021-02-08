package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.javislaptop.binance.pumper.UnusualVolumeDetector;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class HistoricalPumpDetector {

    public static final int MILLIS_TO_GROUP = 100; // 60000 = 1 minute, 1000 = 1 second
    private final BinanceApiRestClient binance;

    public HistoricalPumpDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    public void showPumps() {
        LocalDateTime from = LocalDateTime.of(2021, 2, 5, 20, 59, 0);
        LocalDateTime to = LocalDateTime.of(2021, 2, 5, 21, 1, 0);


        Duration duration = Duration.ofHours(1);
        LocalDateTime currentStart = from;
        LocalDateTime currentEnd = from.plus(duration);
        do {
            List<AggTrade> skybtc = binance.getAggTrades("VIBBTC", null, 100000, currentStart.toInstant(ZoneOffset.UTC).toEpochMilli(), currentEnd.toInstant(ZoneOffset.UTC).toEpochMilli());

            Map<Instant, List<AggTrade>> occurrences = skybtc
                    .stream()
                    .collect(
                            Collectors.groupingBy(
                                    trade -> Instant.ofEpochMilli(trade.getTradeTime() - (trade.getTradeTime() % MILLIS_TO_GROUP))
                            ));

            occurrences.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(t -> new PumpData(t.getKey(), t.getValue()))
                    .filter(t -> t.getTrades() > 5)
                    .forEach(e -> System.out.println(String.format("%s: Volume: %.5f, Ratio: %.5f, Trades: %s. Buys: %d, Sell: %d, Unknown: %d. Initial Price: %.8f. Final price: %.8f", e.getWhen(), e.getVolume(), e.getMakerRatio(), e.getTrades(), e.getBuys(), e.getSells(), e.getBuyOrSell(), e.getInitialPrice(), e.getFinalPrice())));

            currentStart = currentEnd;
            currentEnd = currentStart.plus(duration);
        } while (currentEnd.compareTo(to) <= 0);



    }
}

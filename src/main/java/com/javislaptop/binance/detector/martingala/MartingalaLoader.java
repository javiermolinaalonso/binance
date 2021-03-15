package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.domain.Candlestick;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MartingalaLoader {

    private final Binance binance;

    public MartingalaLoader(Binance binance) {
        this.binance = binance;
    }

    public Map<String, List<Candlestick>> load(LocalDate localFrom, LocalDate localTo, List<String> tradingCurrencies, String baseCurrency, String interval) {
        List<String> symbols = tradingCurrencies.stream().map(t -> t + baseCurrency).collect(Collectors.toList());
        return load(localFrom, localTo, symbols, interval);
    }


    public Map<String, List<Candlestick>> load(LocalDate localFrom, LocalDate localTo, List<String> symbols, String interval) {
        Instant from = localFrom.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = localTo.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Map<String, List<Candlestick>> data = symbols.stream().collect(Collectors.toMap(t -> t, t -> binance.getCandlesticks(t, from, to, interval)));
//        if (data.values().stream().map(List::size).distinct().count() > 1) {
//            throw new RuntimeException("The data is different for each pair, please improve the algorithm");
//        }
        return data;

    }
}

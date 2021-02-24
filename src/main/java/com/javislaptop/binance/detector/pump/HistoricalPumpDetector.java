package com.javislaptop.binance.detector.pump;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.PrecisionNum;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class HistoricalPumpDetector {

    private final Binance binance;

    public HistoricalPumpDetector(Binance binance) {
        this.binance = binance;
    }

    public void execute() {
        String symbol = "NXSBTC";
        List<Bar> candlesticks = binance.getWeekBar(symbol, Instant.now(Clock.system(ZoneId.of("Europe/Madrid"))));

        List<Bar> buyoffs = candlesticks.stream().filter(c -> c.getHighPrice().dividedBy(c.getOpenPrice()).isGreaterThan(PrecisionNum.valueOf(2))).collect(Collectors.toList());
        List<Bar> minuteBars = buyoffs.stream().flatMap(b -> binance.getMinuteBar(symbol, b.getBeginTime(), b.getEndTime()).stream()).collect(Collectors.toList());

        List<Bar> superIncreases = minuteBars.stream().filter(c -> c.getHighPrice().dividedBy(c.getOpenPrice()).isGreaterThan(PrecisionNum.valueOf(1.2))).collect(Collectors.toList());

        List<AggTrade> trades = superIncreases.stream().flatMap(s -> binance.getTrades(symbol, s.getBeginTime().toInstant(), s.getEndTime().toInstant()).stream()).collect(Collectors.toList());

        trades.forEach(System.out::println);
    }
}

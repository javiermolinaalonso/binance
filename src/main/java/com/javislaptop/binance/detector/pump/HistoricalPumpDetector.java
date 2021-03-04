package com.javislaptop.binance.detector.pump;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.ta.MultipleEnterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.candles.ThreeBlackCrowsIndicator;
import org.ta4j.core.indicators.candles.ThreeWhiteSoldiersIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.*;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class HistoricalPumpDetector {

    private static final Logger logger = LogManager.getLogger(PumpInstantDetector.class);

    private final Binance binance;

    public HistoricalPumpDetector(Binance binance) {
        this.binance = binance;
    }

    public void execute() {
        String symbol = "ETHBTC";
        List<Bar> bars = binance.getWeekBar(symbol, Instant.now(Clock.system(ZoneId.of("Europe/Madrid"))));
        BaseBarSeries series = new BaseBarSeriesBuilder().withName(symbol).withBars(bars).build();
        ThreeBlackCrowsIndicator threeBlackCrowsIndicator = new ThreeBlackCrowsIndicator(series, 3, 1.5);
        Rule buyRule = new BooleanIndicatorRule(threeBlackCrowsIndicator);

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        StopGainRule stopGainRule = new StopGainRule(closePriceIndicator, PrecisionNum.valueOf(5));
        StopLossRule stopLossRule = new StopLossRule(closePriceIndicator, PrecisionNum.valueOf(10));
        ThreeWhiteSoldiersIndicator threeWhiteSoldiersIndicator = new ThreeWhiteSoldiersIndicator(series, 3, PrecisionNum.valueOf(1.2));
        BooleanIndicatorRule sellRule = new BooleanIndicatorRule(threeWhiteSoldiersIndicator);
        Strategy strategy = new BaseStrategy(buyRule, sellRule);

        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord run = manager.run(strategy);
        run.getTrades().forEach(System.out::println);
    }

    private void foo() {
        // Porque el dia 08/01 12:00 no se detecta???
        // Porque el dia 11/01 22:00 no se detecta???
        // porque detecta el dia 09/01 a las 16.00????

//        for (int i = 0; i < bars.size(); i++) {
//            boolean satisfied = buyRule.isSatisfied(i);
//            if (satisfied) {
//                logger.info("Purchase at {} at price {}", bars.get(i).getEndTime(), bars.get(i).getClosePrice());
//            }
//        }
    }
}

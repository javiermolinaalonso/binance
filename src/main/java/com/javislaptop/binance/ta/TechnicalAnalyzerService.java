package com.javislaptop.binance.ta;

import com.binance.api.client.domain.market.Candlestick;
import com.javislaptop.binance.api.Binance;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.candles.BearishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TechnicalAnalyzerService {

    private final Binance binance;

    public TechnicalAnalyzerService(Binance binance) {
        this.binance = binance;
    }

    public void execute(String symbol) {
        List<Bar> bars = mapCandlestickToBar(symbol);
        BaseBarSeries series = new BaseBarSeriesBuilder().withName(symbol).withBars(bars).build();

        // First we generate the indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
        SMAIndicator sma5Indicator = new SMAIndicator(closePrice, 5);

        BearishEngulfingIndicator bearishEngulfingIndicator = new BearishEngulfingIndicator(series);
        BullishEngulfingIndicator bullishEngulfingIndicator = new BullishEngulfingIndicator(series);


        //Then we define the strategy
        BooleanIndicatorRule bearishEngulfingRule = new BooleanIndicatorRule(bearishEngulfingIndicator);
        IsHighestRule highestRuleDaily = new IsHighestRule(openPriceIndicator, 24);
        Rule buyRule = new BooleanIndicatorRule(bullishEngulfingIndicator);

        Rule sellRule = new AndRule(highestRuleDaily, bearishEngulfingRule);
        Strategy strategy = new BaseStrategy(buyRule, sellRule);

        //And we perform the backtest
        // Running our juicy trading strategy...
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(strategy);
        System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());

        AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
        System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
// Getting the reward-risk ratio
        AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
        System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

// Total profit of our strategy
// vs total profit of a buy-and-hold strategy
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
        System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
    }

    private List<Bar> mapCandlestickToBar(String symbol) {
        List<Candlestick> lastHour = binance.getLastWeek(symbol, Instant.now(Clock.systemUTC()));
        return lastHour.stream()
                .map(c -> new BaseBar(
                        Duration.ofHours(1),
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(c.getCloseTime()), ZoneId.of("Europe/Paris")),
                        c.getOpen(),
                        c.getHigh(),
                        c.getLow(),
                        c.getClose(),
                        c.getVolume(),
                        "0",
                        c.getNumberOfTrades().toString(),
                        PrecisionNum::valueOf)
                ).collect(Collectors.toList());
    }
}

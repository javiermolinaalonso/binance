package com.javislaptop.binance.ta;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;

public class MultipleEnterStrategy extends BaseStrategy {
    public MultipleEnterStrategy(Rule entryRule, Rule exitRule) {
        super(entryRule, exitRule);
    }

    @Override
    public boolean shouldOperate(int index, TradingRecord tradingRecord) {
        return shouldEnter(index, tradingRecord);
    }
}

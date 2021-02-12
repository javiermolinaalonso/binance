package com.javislaptop.binance.detector.pump;

import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.market.AggTrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class PumpData {

    private final Instant when;
    private final Double volume;
    private final BigDecimal initialPrice;
    private final BigDecimal finalPrice;
    private final Integer trades;
    private final Integer makerTrades;
    private Integer buys = 0;
    private Integer sells = 0;
    private Integer buyOrSell = 0;

    public PumpData(Instant when, List<? extends AggTrade> trades) {
        trades.sort(Comparator.comparing(AggTrade::getTradeTime));
        this.when = when;
        this.volume = trades.stream().mapToDouble(t -> new BigDecimal(t.getQuantity()).doubleValue()).sum();
        this.trades = trades.size();
        this.makerTrades = trades.stream().mapToInt(this::makerTrades).sum();
        this.initialPrice = new BigDecimal(trades.get(0).getPrice());
        this.finalPrice = new BigDecimal(trades.get(trades.size() - 1).getPrice());
        buildOrders(trades);
    }

    public PumpData(AggTradeEvent t) {
        this(Instant.ofEpochMilli(t.getEventTime()), List.of(t));
    }

    private void buildOrders(List<? extends AggTrade> lnkList) {
        if (lnkList.size() < 2) {
            return;
        }
        ListIterator<? extends AggTrade> aggTradeListIterator = lnkList.listIterator(1);
        AggTrade previous = aggTradeListIterator.previous();
        do {
            AggTrade current = aggTradeListIterator.next();
            int priceIncrease = isPriceIncrease(previous, current);
            if (priceIncrease > 0) {
                buys++;
            } else if (priceIncrease < 0) {
                sells++;
            } else {
                buyOrSell++;
            }
            previous = current;
        }while(aggTradeListIterator.hasNext());
    }

    public Integer getBuys() {
        return buys;
    }

    public Integer getSells() {
        return sells;
    }

    public Integer getBuyOrSell() {
        return buyOrSell;
    }

    private int isPriceIncrease(AggTrade previous, AggTrade next) {
        return extractPrice(next).compareTo(extractPrice(previous));
    }

    public Instant getWhen() {
        return when;
    }

    public Double getVolume() {
        return volume;
    }

    public Integer getTrades() {
        return trades;
    }

    public Integer getMakerTrades() {
        return makerTrades;
    }

    public Double getMakerRatio() {
        return (double) makerTrades / (double) trades;
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    private int makerTrades(AggTrade aggTrade) {
        if (aggTrade.isBuyerMaker()) {
            return 1;
        } else {
            return 0;
        }
    }

    public static Double extractVolume(String price, String quantity) {
        return new BigDecimal(price).multiply(new BigDecimal(quantity)).doubleValue();
    }

    private BigDecimal extractPrice(AggTrade aggTrade) {
        return new BigDecimal(aggTrade.getPrice());
    }

    public double getPriceIncrease() {
        return finalPrice.subtract(initialPrice).divide(initialPrice, 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    @Override
    public String toString() {
        return String.format("%s: Volume: %.5f, Ratio: %.5f, Trades: %s. Buys: %d, Sell: %d, Unknown: %d. Initial Price: %.8f. Final price: %.8f", when, volume, getMakerRatio(), trades, buys, sells, buyOrSell, initialPrice, finalPrice);
    }
}

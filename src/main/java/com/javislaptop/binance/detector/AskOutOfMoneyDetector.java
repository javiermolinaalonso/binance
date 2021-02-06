package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.OrderBook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.OptionalDouble;

import static java.lang.Double.parseDouble;

@Service
public class AskOutOfMoneyDetector {

    private final BinanceApiRestClient binance;

    public AskOutOfMoneyDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    public boolean isOrderBookOverAskOutOfMoney(String symbol, int distance) {
        double distanceFactor = 1d + ((double) distance / 100d);
        OrderBook orderBook = binance.getOrderBook(symbol, 100);
        if (orderBook.getAsks().isEmpty()) {
            return false;
        }
        List<AggTrade> lastTrades = binance.getAggTrades(symbol);
        double average = lastTrades.stream().mapToDouble(t -> parseDouble(t.getPrice())).average().orElse(0d);
        double orderBookLimit = average * distanceFactor;
//        System.out.println(String.format("Average price of %s. Will retrieve percentage of orders over %s", average, orderBookLimit));
        double moneyOom = orderBook.getAsks().stream().filter(ask -> parseDouble(ask.getPrice()) > orderBookLimit).mapToDouble(ask -> parseDouble(ask.getQty()) * parseDouble(ask.getPrice())).sum();
        double moneyIm = orderBook.getAsks().stream().filter(ask -> parseDouble(ask.getPrice()) <= orderBookLimit).mapToDouble(ask -> parseDouble(ask.getQty()) * parseDouble(ask.getPrice())).sum();

        double percentageOom = moneyOom / (moneyIm + moneyOom);
        double percentageIm = moneyIm / (moneyIm + moneyOom);

//        System.out.println(String.format("Percentage In the money: %s. Percentage Out of money: %s", percentageIm, percentageOom));
        return percentageOom > percentageIm;
    }
}

package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.javislaptop.binance.pumper.UnusualVolumeDetector;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PumpDetector {

    private final BinanceApiRestClient binance;
    private final UnusualVolumeDetector volumeDetector;
    private final AskOutOfMoneyDetector askOutOfMoneyDetector;

    public PumpDetector(BinanceApiRestClient binance, UnusualVolumeDetector volumeDetector, AskOutOfMoneyDetector askOutOfMoneyDetector) {
        this.binance = binance;
        this.volumeDetector = volumeDetector;
        this.askOutOfMoneyDetector = askOutOfMoneyDetector;
    }

    public void showPumps() {
        List<String> btcPairs = binance.getExchangeInfo().getSymbols().stream()
                .filter(symbol -> symbol.getSymbol().endsWith("BTC"))
                .map(SymbolInfo::getSymbol)
                .collect(Collectors.toList());

        LocalDateTime from = LocalDateTime.of(2021, 2, 3, 20, 50, 0);
        LocalDateTime to = LocalDateTime.of(2021, 2, 3, 21, 00, 10);
        List<AggTrade> skybtc = binance.getAggTrades("SKYBTC", null, 100000, from.toInstant(ZoneOffset.UTC).toEpochMilli(), to.toInstant(ZoneOffset.UTC).toEpochMilli());

        skybtc.stream().forEach(trade -> {
            Instant instant = Instant.ofEpochMilli(trade.getTradeTime());
            boolean isMaker = trade.isBuyerMaker();
            if (!isMaker) {
                System.out.println(String.format("%s. %s First: %s. Last: %s", instant.toString(), trade.getPrice(), trade.getFirstBreakdownTradeId(), trade.getLastBreakdownTradeId()));
            }
        });
//        btcPairs.forEach(pair -> {
//            boolean orderBookOverAskOutOfMoney = askOutOfMoneyDetector.isOrderBookOverAskOutOfMoney(pair, 50);
//            if (orderBookOverAskOutOfMoney) {
//
//                System.out.println(pair);
//            }
//        });

    }
}

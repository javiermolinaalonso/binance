package com.javislaptop.binance.detector.coinpair;

import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import com.binance.api.client.domain.market.OrderBook;
import com.javislaptop.binance.api.Binance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
public class CoinPairAnalyzer {

    private static final Logger logger = LogManager.getLogger(CoinPairAnalyzer.class);

    private final Binance binance;
    private final List<String> mainCoins = List.of("BTC", "ETH", "BNB", "BUSD");

    private Map<String, List<SymbolInfo>> arbitrageMap;

    public CoinPairAnalyzer(Binance binance) {
        this.binance = binance;
        this.arbitrageMap = new HashMap<>();
    }

    public void execute() {
//        initializeArbitrageMap();
//        String coin = "MATIC";
//        List<SymbolInfo> values = arbitrageMap.get(coin);

        //P.ej Comprar MATICBTC y vender BTCBNB

        BigDecimal amountOfBtc = new BigDecimal(1);
        BigDecimal buyPriceMaticBtc = binance.getSellPrice("PHBBTC");
        BigDecimal amountOfMatic = amountOfBtc.divide(buyPriceMaticBtc, 0, RoundingMode.DOWN);
        BigDecimal sellPriceMaticBnb = binance.getBuyPrice("PHBTUSD");
        BigDecimal amountOfBnb = amountOfMatic.multiply(sellPriceMaticBnb);
        BigDecimal orderBookBtcBnb = binance.getSellPrice("BTCTUSD");
        BigDecimal amountOfBtcAfterTrades = amountOfBnb.divide(orderBookBtcBnb, 8, RoundingMode.DOWN);
        System.out.println(amountOfBtcAfterTrades);
    }

    private void initializeArbitrageMap() {
        binance.getExchangeInfo().getSymbols()
                .stream()
                .filter(t -> t.getStatus() == SymbolStatus.TRADING)
                .filter(t -> mainCoins.stream().anyMatch(mainCoin -> t.getSymbol().contains(mainCoin)))
                .forEach(symbolInfo -> {
                    mainCoins.stream()
                            .filter(mc -> symbolInfo.getSymbol().contains(mc))
                            .findAny()
                            .ifPresent(mainCoin -> {
                                arbitrageMap.computeIfAbsent(symbolInfo.getSymbol().replaceAll(mainCoin, ""), k -> new ArrayList<>()).add(symbolInfo);
                            });
                });
        logger.info("Arbitrage map initialized: {}", arbitrageMap);
    }
}

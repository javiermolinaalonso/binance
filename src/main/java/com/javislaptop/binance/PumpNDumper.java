package com.javislaptop.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Trade;

import java.util.Scanner;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

public class PumpNDumper {

    private final String btcAmount;
    private final BinanceApiRestClient binance;
    private final Scanner scanner;

    public PumpNDumper(String btcAmount, BinanceApiRestClient binance, Scanner scanner) {
        this.btcAmount = btcAmount;
        this.binance = binance;
        this.scanner = scanner;
    }

    public void execute() {
        System.out.println("Choose the ticker: ");
        String ticker = scanner.nextLine();
        String pair = ticker.toUpperCase() + "BTC";
        System.out.println("Purchasing " + pair);
        NewOrderResponse buyResponse = binance.newOrder(marketBuy(pair, null).quoteOrderQty(btcAmount).newOrderRespType(NewOrderResponseType.FULL));
        String buyprice = buyResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");

        System.out.println(String.format("Purchased %s %s at %s", buyResponse.getExecutedQty(), ticker.toUpperCase(), buyprice));

        System.out.println("Purchased at");
        System.out.println("Press enter for sell");
        scanner.nextLine();
        NewOrderResponse sellResponse = binance.newOrder(marketSell(pair, buyResponse.getExecutedQty()).newOrderRespType(NewOrderResponseType.FULL));

        String prices = sellResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");
        System.out.println(String.format("Sold %s %s at %s", sellResponse.getExecutedQty(), ticker.toUpperCase(), prices));
    }
}

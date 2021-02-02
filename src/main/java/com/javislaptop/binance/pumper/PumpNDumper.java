package com.javislaptop.binance.pumper;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Trade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Scanner;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

@Service
public class PumpNDumper {

    private final BinanceApiRestClient binance;
    private final Scanner scanner;
    private final DecimalFormat formatter;

    public PumpNDumper(BinanceApiRestClient binance, Scanner scanner) {
        this.binance = binance;
        this.scanner = scanner;
        this.formatter = new DecimalFormat("0.00000000");
    }

    public void execute() {
        String btcAmount = getAmount();
        System.out.println(String.format("You will bet %s BTC", btcAmount));
        System.out.println("Choose the ticker: ");
        String ticker = scanner.nextLine();
        String pair = ticker.toUpperCase() + "BTC";
        System.out.println("Purchasing " + pair);
        NewOrderResponse buyResponse = binance.newOrder(marketBuy(pair, null).quoteOrderQty(btcAmount).newOrderRespType(NewOrderResponseType.FULL));
        String buyprice = buyResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");

        System.out.println(String.format("Purchased %s %s at %s", buyResponse.getExecutedQty(), ticker.toUpperCase(), buyprice));

        System.out.println("Press enter for sell");
        scanner.nextLine();
        NewOrderResponse sellResponse = binance.newOrder(marketSell(pair, buyResponse.getExecutedQty()).newOrderRespType(NewOrderResponseType.FULL));

        String prices = sellResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");
        System.out.println(String.format("Sold %s %s at %s", sellResponse.getExecutedQty(), ticker.toUpperCase(), prices));
    }

    private String getAmount() {
        AssetBalance assetBalance = binance.getAccount().getAssetBalance("BTC");
        return formatter.format(readAmount(assetBalance)).replaceAll(",", ".");
    }

    private BigDecimal readAmount(AssetBalance assetBalance) {
        BigDecimal maxAmount =  new BigDecimal(assetBalance.getFree()).multiply(BigDecimal.valueOf(0.99));
        System.out.println(String.format("Choose the amount of BTC (Max: %s) Enter for MAX", formatter.format(maxAmount)));
        String btcAmount = scanner.nextLine();
        BigDecimal amount;
        if (StringUtils.isBlank(btcAmount)) {
            amount = maxAmount;
        } else {
            try {
                amount = new BigDecimal(btcAmount);
            } catch (Exception e) {
                System.out.println("Wrong input, please retry");
                return readAmount(assetBalance);
            }
        }
        return maxAmount.min(amount);
    }
}

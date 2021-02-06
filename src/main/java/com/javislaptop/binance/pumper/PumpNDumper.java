package com.javislaptop.binance.pumper;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.market.OrderBook;
import com.javislaptop.binance.detector.AskOutOfMoneyDetector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

import static com.binance.api.client.domain.account.NewOrder.*;

@Service
public class PumpNDumper {

    private final BinanceApiRestClient binance;
    private final Scanner scanner;
    private final AskOutOfMoneyDetector askOutOfMoneyDetector;
    private final DecimalFormat buyFormatter;
    private final DecimalFormat sellFormatter;

    public PumpNDumper(BinanceApiRestClient binance, Scanner scanner, AskOutOfMoneyDetector askOutOfMoneyDetector) {
        this.binance = binance;
        this.scanner = scanner;
        this.askOutOfMoneyDetector = askOutOfMoneyDetector;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
        otherSymbols.setDecimalSeparator('.');
        this.buyFormatter = new DecimalFormat("0.00000000", otherSymbols);
        this.sellFormatter = new DecimalFormat("0.00000000", otherSymbols);
    }

    public void execute() {
        String ticker = getTicker();
        String symbol = getSymbol(ticker);

        String btcAmount = getAmount();
        Optional<BigDecimal> limit = getLimit();



        NewOrderResponse buyResponse = binance.newOrder(marketBuy(symbol, null).quoteOrderQty(btcAmount).newOrderRespType(NewOrderResponseType.FULL));
        String buyprice = buyResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");
        String qty = buyFormatter.format(buyResponse.getFills().stream().map(Trade::getQty).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add));
        System.out.println(String.format("Purchased %s %s at %s", qty, ticker.toUpperCase(), buyprice));

        NewOrderResponse marketSell = null;
        if (limit.isPresent()) {
            String targetPrice = sellFormatter.format(new BigDecimal(buyprice).multiply(limit.get()));
            System.out.println("Selling at target price " + targetPrice);
            marketSell = binance.newOrder(limitSell(symbol, TimeInForce.GTC, qty, targetPrice));
            System.out.println("Order set, remember it won't be executed if price does not reach the target price.");
            System.out.println("Order will be cancelled automatically if you press enter for sell");
            System.out.println("Cancel manually to send a market sell order from binance client");
        }

        System.out.println("Press enter for sell");
        scanner.nextLine();

        if (marketSell != null) {
            binance.cancelOrder(new CancelOrderRequest(symbol, marketSell.getOrderId()));
        }
        NewOrderResponse sellResponse = binance.newOrder(marketSell(symbol, buyResponse.getExecutedQty()).newOrderRespType(NewOrderResponseType.FULL));

        String prices = sellResponse.getFills().stream().map(Trade::getPrice).findFirst().orElse("");
        System.out.println(String.format("Sold %s %s at %s", sellResponse.getExecutedQty(), ticker.toUpperCase(), prices));
    }

    private String getSymbol(String ticker) {
        String symbol = ticker.toUpperCase() + "BTC";
        System.out.println("Purchasing " + symbol);
        return symbol;
    }

    private String getTicker() {
        System.out.println("Choose the ticker: ");
        return scanner.nextLine();
    }

    private Optional<BigDecimal> getLimit() {
        System.out.println("Where do you want the limit? Please use percentage, keep blank for market order. Example: 100 will be doubling the position, 200 tripling the position");
        String limit = scanner.nextLine();
        if (StringUtils.isEmpty(limit)) {
            System.out.println("No automatic sell order will be sent, good luck");
            return Optional.empty();
        }
        try {
            int l = Integer.parseInt(limit);
            if (l <= 0) {
                System.out.println("Number should be positive");
                return getLimit();
            }
            BigDecimal result = new BigDecimal(1).add(new BigDecimal(l).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
            System.out.println("The limit order will be placed at " + l + "%. So the sell order will be placed at purchasePrice * " + buyFormatter.format(result));
            return Optional.of(result);
        } catch (Exception e) {
            System.out.println("Please write a number.");
            return getLimit();
        }
    }

    private String getAmount() {
        AssetBalance assetBalance = binance.getAccount().getAssetBalance("BTC");
        String result = buyFormatter.format(readAmount(assetBalance)).replaceAll(",", ".");
        System.out.println(String.format("You will bet %s BTC", result));

        AssetBalance bnbBalance = binance.getAccount().getAssetBalance("BNB");
        System.out.println(String.format("You have %s BNB, please make sure you have enough BNB to pay the fees. Keep at least 1 percent", bnbBalance.getFree()));
        return result;
    }

    private BigDecimal readAmount(AssetBalance assetBalance) {
        BigDecimal maxAmount =  new BigDecimal(assetBalance.getFree());
        System.out.println(String.format("Choose the amount of BTC (Max: %s) Enter for MAX", buyFormatter.format(maxAmount)));
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

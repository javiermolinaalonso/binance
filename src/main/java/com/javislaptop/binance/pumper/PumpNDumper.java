package com.javislaptop.binance.pumper;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.javislaptop.binance.api.Binance;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

@Service
public class PumpNDumper {

    private final Binance binance;
    private final Scanner scanner;

    public PumpNDumper(Binance binance, Scanner scanner) {
        this.binance = binance;
        this.scanner = scanner;
    }

    public void execute() {
        BigDecimal btcAmount = getAmount();
        Optional<BigDecimal> buyLimit = getBuyLimit();
        Optional<BigDecimal> sellLimit = getSellLimit();
        String ticker = getTicker();
        String symbol = getSymbol(ticker);

        Order buyResponse = purchase(btcAmount, buyLimit, symbol);
        if (buyResponse == null) return;

        Optional<Trade> trade = binance.getTrade(symbol, buyResponse.getOrderId());
        trade.ifPresentOrElse(
                t -> System.out.println(String.format("Purchased %s %s at %s", buyResponse.getExecutedQty(), ticker.toUpperCase(), t.getPrice())),
                () -> System.out.println(String.format("Purchased %s %s at %s", buyResponse.getExecutedQty(), ticker.toUpperCase(), buyResponse.getPrice()))
        );


        Order marketSell = null;
        if (sellLimit.isPresent()) {
            BigDecimal targetPrice = new BigDecimal(buyResponse.getPrice()).multiply(sellLimit.get());

            marketSell = binance.sellLimit(symbol, buyResponse.getExecutedQty(), targetPrice);
            System.out.println("Order set, remember it won't be executed if price does not reach the target price.");
            System.out.println("Order will be cancelled automatically if you press enter for sell");
            System.out.println("Cancel manually to send a market sell order from binance client");
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                binance.printPrices(symbol, trade.map(Trade::getPrice).orElse(buyResponse.getPrice()));
            }
        }, 0, 10);

        System.out.println("Press enter for sell");
        scanner.nextLine();

        if (marketSell != null) {
            binance.cancelOrder(symbol, marketSell.getOrderId());
        }
        Order sellResponse = binance.sellMarket(symbol, new BigDecimal(buyResponse.getExecutedQty()));

        System.out.println(String.format("Sold %s %s at %s", sellResponse.getExecutedQty(), ticker.toUpperCase(), sellResponse.getPrice()));
    }

    private Order purchase(BigDecimal btcAmount, Optional<BigDecimal> buyLimit, String symbol) {
        Order buyResponse = null;
        if (buyLimit.isPresent()) {
            Optional<BigDecimal> averagePrice = binance.getAveragePrice(symbol);
            if (averagePrice.isPresent()) {
                BigDecimal buyPrice = buyLimit.get().multiply(averagePrice.get());
                buyResponse = binance.buyLimit(symbol, btcAmount, buyPrice);
            }
        } else {
            buyResponse = binance.buyMarket(symbol, btcAmount);
        }

        if (buyResponse == null || buyResponse.getStatus() == OrderStatus.NEW) {
            System.out.println("Purchase was not successful, cancelling order and skipping pump");
            if (buyResponse == null) {
                System.out.println("There was an error with binance api, please cancel order manually");
            } else {
                binance.cancelOrder(symbol, buyResponse.getOrderId());
            }
            return null;
        }

        return buyResponse;
    }


    private String getSymbol(String ticker) {
        String symbol = ticker.toUpperCase() + "BTC";
        System.out.println("Purchasing " + symbol);
        return symbol;
    }

    private String getTicker() {
        System.out.println("Choose the ticker (After you write the ticker the purchase will start): ");
        return scanner.nextLine();
    }


    private Optional<BigDecimal> getBuyLimit() {
        System.out.println("Where do you want the buy limit? This will limit the purchase amount, if the current price is above then the order won't be filled." +
                "Please use percentage, keep blank for purchase without protection. " +
                "Example: 100 will limit the purchase to double the average price");
        return scanForNumber("No protection will be set, good luck", this::getBuyLimit);
    }

    private Optional<BigDecimal> getSellLimit() {
        System.out.println("Where do you want the sell limit? Please use percentage, keep blank for market order. Example: 100 will be doubling the position, 200 tripling the position");
        return scanForNumber("No automatic sell order will be sent, good luck", this::getSellLimit);
    }

    private Optional<BigDecimal> scanForNumber(String s, Supplier<Optional<BigDecimal>> function) {
        String limit = scanner.nextLine();
        if (StringUtils.isEmpty(limit)) {
            System.out.println(s);
            return Optional.empty();
        }
        try {
            int l = Integer.parseInt(limit);
//            if (l <= 0) {
//                System.out.println("Number should be positive");
//                return function.get();
//            }
            BigDecimal result = new BigDecimal(1).add(new BigDecimal(l).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
            System.out.println("The limit order will be placed at " + l + "%. So the sell order will be placed at purchasePrice * " + result.toPlainString());
            return Optional.of(result);
        } catch (Exception e) {
            System.out.println("Please write a number.");
            return function.get();
        }
    }
    private BigDecimal getAmount() {
        String assetBalance = binance.getAssetBalance("BTC");
        BigDecimal result = readAmount(assetBalance);
        System.out.println(String.format("You will bet %s BTC", result.toPlainString()));

        String bnbBalance = binance.getAssetBalance("BNB");
        System.out.println(String.format("You have %s BNB, please make sure you have enough BNB to pay the fees. Keep at least 1 percent", bnbBalance));
        return result;
    }

    private BigDecimal readAmount(String assetBalance) {
        BigDecimal maxAmount =  new BigDecimal(assetBalance).multiply(new BigDecimal("0.99"));
        System.out.println(String.format("Choose the amount of BTC (Max: %s) Enter for MAX", maxAmount.toPlainString()));
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

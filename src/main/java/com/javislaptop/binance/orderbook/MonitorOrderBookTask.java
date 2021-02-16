package com.javislaptop.binance.orderbook;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBook;
import com.javislaptop.binance.orderbook.domain.BinanceOrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class MonitorOrderBookTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(MonitorOrderBookTask.class);
    private static final BigDecimal RESISTANCE_THRESHOLD = new BigDecimal("0.05");
    private static final BigDecimal MAX_DISTANCE_TO_RESISTANCE = new BigDecimal("0.001");
    private static final BigDecimal MIN_GAP = new BigDecimal("0.005");
    private static final BigDecimal MAX_GAP = new BigDecimal("0.05");
    private static final BigDecimal BTC_PER_TRADE = new BigDecimal("0.0005");

    private final StreamDataStorage storage;
    private final String symbol;
    private final Binance binance;
    private final BinanceDataStreamer streamer;
    private final SymbolInfo symbolInfo;


    public MonitorOrderBookTask(StreamDataStorage storage, String symbolInfo, Binance binance, BinanceDataStreamer streamer) {
        this.storage = storage;
        this.symbol = symbolInfo;
        this.binance = binance;
        this.streamer = streamer;
        this.symbolInfo = binance.getSymbolInfo(symbol);
        streamer.enableBookTickerEvents(symbolInfo);
    }

    @Override
    public void run() {
        try {
//            List<Order> openOrders = binance.getAccountOrders(symbol)
//                    .stream()
//                    .filter(o -> o.getStatus() != OrderStatus.CANCELED)
//                    .filter(o -> o.getStatus() != OrderStatus.EXPIRED)
//                    .filter(o -> o.getStatus() != OrderStatus.REJECTED)
//                    .filter(o -> o.getStatus() != OrderStatus.FILLED)
//                    .collect(Collectors.toList());
            List<Order> openOrders = binance.getOpenOrders(symbol);
            BinanceOrderBook book = binance.getOrderBook(symbol, 50);

            Optional<BinanceOrderBookEntry> floor = book.getFloor(RESISTANCE_THRESHOLD); //TODO detect floor and resistance grouping by units
            if (openOrders.size() > 1) {
                logger.warn("For some reason there is more than one open order, cancelling all of them which are buy");
                openOrders.stream().filter(o -> o.getSide() == OrderSide.BUY).forEach(o -> binance.cancelOrder(symbol, o.getOrderId(), null));
            }

            if (openOrders.isEmpty()) {
                book.getDistanceBetweenFloorAndResistance(RESISTANCE_THRESHOLD)
                        .filter(d -> d.compareTo(MIN_GAP) > 0)
                        .filter(d -> d.compareTo(MAX_GAP) < 0)
                        .ifPresentOrElse(s -> {
                            binance.buyLimit(symbol, BTC_PER_TRADE, floor.map(BinanceOrderBookEntry::getPrice).get().add(new BigDecimal(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize())));
                        },
                                () -> logger.info("There is no floor or the distance is too small or too high.")); //TODO improve this logic to add purchase in case there is a clear floor
            } else {
                Optional<Order> openBuyOrder = openOrders.stream()
                        .filter(o -> o.getStatus() != OrderStatus.FILLED)
                        .filter(o -> o.getSide() == OrderSide.BUY)
                        .findAny();
                openBuyOrder.ifPresent(o -> {
                    floor.ifPresentOrElse(
                            f -> {
                                BigDecimal floorPrice = f.getPrice();
                                BigDecimal orderPrice = new BigDecimal(o.getPrice());
                                if (orderPrice.compareTo(floorPrice) < 0) {
                                    //There is a new floor before our purchase price
                                    logger.info("There is a new floor before our price. Moving limit order");
                                    binance.cancelOrder(o);
                                    binance.buyLimit(symbol, BTC_PER_TRADE, floorPrice.add(new BigDecimal(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize())));
                                } else {
                                    if (orderPrice.subtract(floorPrice).divide(floorPrice, 8, RoundingMode.DOWN).compareTo(MAX_DISTANCE_TO_RESISTANCE) > 0) {
                                        logger.info("The floor has gone too low. Moving limit order");
                                        binance.cancelOrder(o);
                                        binance.buyLimit(symbol, BTC_PER_TRADE, floorPrice.add(new BigDecimal(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize())));
                                    } else {
                                        logger.info("Order book didn't change");
                                    }
                                }
                            },
                            () -> {
                                logger.info("The floor has gone, cancelling order");
                                binance.cancelOrder(o); //The resistance has gone
                            }
                    );
                });
                Optional<Order> openSellOrder = openOrders.stream()
                        .filter(o -> o.getStatus() != OrderStatus.FILLED)
                        .filter(o -> o.getSide() == OrderSide.SELL)
                        .findAny();
                openSellOrder.ifPresent(
                        o -> {
                            book.getResistance(RESISTANCE_THRESHOLD).ifPresent(
                                    resistance -> {
                                        BigDecimal resistancePrice = resistance.getPrice();
                                        BigDecimal orderPrice = new BigDecimal(o.getPrice());
                                        if (resistancePrice.compareTo(orderPrice) < 0) {
                                            //The resistance has gone beyond our price
                                            binance.cancelOrder(o);
                                            binance.sellLimit(symbol, o.getOrigQty(), resistancePrice.subtract(new BigDecimal(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize())));
                                        } else {
                                            if (resistancePrice.subtract(orderPrice).divide(orderPrice, 8, RoundingMode.DOWN).compareTo(MAX_DISTANCE_TO_RESISTANCE) > 0) {
                                                //The resistance has gone too far
                                                binance.cancelOrder(o);
                                                binance.sellLimit(symbol, o.getOrigQty(), resistancePrice.subtract(new BigDecimal(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize())));
                                            }
                                        }
                                    }
                            );
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cancel() {
        TimerTask placeOrderTask = new PlaceOrderTask(binance, symbol, storage, streamer);
        new Timer("update-task-" + symbol).schedule(placeOrderTask, 100, 1000);
        return super.cancel();
    }
}

package com.javislaptop.binance.strategy;

import com.binance.api.client.domain.event.BookTickerEvent;
import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import static com.javislaptop.binance.Utils.calculateBenefit;

public abstract class AbstractPurchaseAndSellQuickStrategy implements TradeStrategy {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BinanceDataStreamer dataDownloader;
    private final StreamDataStorage storage;
    final PurchaseAndSellQuickStrategyProperties properties;

    BigDecimal buyPrice;
    BigDecimal sellPrice;

    public AbstractPurchaseAndSellQuickStrategy(BinanceDataStreamer dataDownloader, StreamDataStorage storage, PurchaseAndSellQuickStrategyProperties properties) {
        this.dataDownloader = dataDownloader;
        this.storage = storage;
        this.properties = properties;
    }

    public void execute(String symbol) {
        dataDownloader.disableAggTradeEvents(symbol);
        purchase(symbol);
        dataDownloader.enableBookTickerEvents(symbol);

        TimerTask exitTimeTask = getTimeExitTask(symbol);
        new Timer(symbol).schedule(exitTimeTask, 0, 100);
    }

    private TimerTask getTimeExitTask(String symbol) {
        return new TimerTask() {

            private final Long started = System.currentTimeMillis();

            @Override
            public void run() {
               Optional<BookTickerEvent> bookTickerEvent = storage.readBookTickerEvent(symbol);
               bookTickerEvent.ifPresent(book -> {
                    BigDecimal bidPrice = new BigDecimal(book.getBidPrice());
                    BigDecimal benefitPercent = calculateBenefit(buyPrice, bidPrice);
                    logger.debug(String.format("[%s] Benefit %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), benefitPercent));

                    if (benefitPercent.compareTo(new BigDecimal(properties.getBenefitPercent())) > 0
                            || benefitPercent.compareTo(new BigDecimal(properties.getLossPercent())) < 0
                    ) {
                        sell(symbol, bidPrice);
                        logger.info(String.format("[%s] Sold order for %s at %s with benefit %.2f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, bidPrice, benefitPercent));
                        this.cancel();
                    }
                });

                if ((System.currentTimeMillis() - started) > properties.getTimeout()) {
                    sell(symbol);
                    BigDecimal benefitPercent = calculateBenefit(buyPrice, sellPrice);
                    logger.info(String.format("[%s] Closing trade for %s at %s because of time with benefit %.4f", Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS), symbol, sellPrice, benefitPercent));
                    this.cancel();
                }
            }

            @Override
            public boolean cancel() {
                dataDownloader.disableBookTickerEvents(symbol);
                dataDownloader.enableAggTradeEvents(symbol);
                return super.cancel();
            }
        };
    }

    protected abstract void purchase(String symbol);

    protected abstract void sell(String symbol, BigDecimal bidPrice);

    protected abstract void sell(String symbol);
}

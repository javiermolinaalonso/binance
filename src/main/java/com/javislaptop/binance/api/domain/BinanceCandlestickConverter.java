package com.javislaptop.binance.api.domain;

public class BinanceCandlestickConverter {

    public Candlestick convert(com.binance.api.client.domain.market.Candlestick candlestick) {
        return new Candlestick(
                candlestick.getOpenTime(),
                candlestick.getOpen(),
                candlestick.getHigh(),
                candlestick.getLow(),
                candlestick.getClose(),
                candlestick.getVolume(),
                candlestick.getCloseTime(),
                candlestick.getQuoteAssetVolume(),
                candlestick.getNumberOfTrades(),
                candlestick.getTakerBuyBaseAssetVolume(),
                candlestick.getTakerBuyQuoteAssetVolume()
        );
    }
}

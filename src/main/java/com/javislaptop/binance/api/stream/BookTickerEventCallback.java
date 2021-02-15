package com.javislaptop.binance.api.stream;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.event.BookTickerEvent;
import com.binance.api.client.domain.event.DepthEvent;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.springframework.stereotype.Service;

@Service
public class BookTickerEventCallback implements BinanceApiCallback<DepthEvent> {

    private final StreamDataStorage storage;

    public BookTickerEventCallback(StreamDataStorage storage) {
        this.storage = storage;
    }

    @Override
    public void onResponse(DepthEvent response) {
        storage.add(response);
    }

    @Override
    public void onFailure(Throwable cause) {

    }
}

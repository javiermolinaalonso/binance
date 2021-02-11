package com.javislaptop.binance.api.stream;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.javislaptop.binance.api.stream.storage.StreamDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AggTradeEventCallback implements BinanceApiCallback<AggTradeEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AggTradeEventCallback.class);

    private final StreamDataStorage storage;

    public AggTradeEventCallback(StreamDataStorage storage) {
        this.storage = storage;
    }

    @Override
    public void onResponse(AggTradeEvent response) {
        logger.debug("Received {}", response);
        storage.add(response);
    }

    @Override
    public void onFailure(Throwable cause) {
        logger.error("Failure on agg trade callback", cause);
    }
}

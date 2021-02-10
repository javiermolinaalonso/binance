package com.javislaptop.binance.detector;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@EnableConfigurationProperties(PumpDetectorProperties.class)
public class RealtimePumpDetector {

    private final BinanceApiWebSocketClient binanceWebsocket;
    private final PumpDetectorProperties pumpDetectorProperties;
    private final PumpInstantDetector pumpInstantDetector;

    private final ConcurrentHashMap<String, List<AggTradeEvent>> eventsMap;
    private final ConcurrentHashMap<String, Closeable> currentPumps;
    private final AtomicInteger counter = new AtomicInteger(0);

    public RealtimePumpDetector(BinanceApiWebSocketClient binanceWebsocket, PumpDetectorProperties pumpDetectorProperties, PumpInstantDetector pumpInstantDetector) {
        this.binanceWebsocket = binanceWebsocket;
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.pumpInstantDetector = pumpInstantDetector;
        this.eventsMap = new ConcurrentHashMap<>();
        this.currentPumps = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        BinanceApiCallback<AggTradeEvent> binanceApiCallback = new BinanceApiCallback<>() {

            @Override
            public void onResponse(AggTradeEvent response) {
                eventsMap.get(response.getSymbol()).add(response);
            }

            @Override
            public void onFailure(Throwable cause) {
                System.out.println(cause.getMessage());
            }
        };

        pumpDetectorProperties
                .getSymbols()
                .forEach(
                        t -> {
                            eventsMap.put(t, new ArrayList<>());
                            currentPumps.put(t, binanceWebsocket.onAggTradeEvent(t.toLowerCase(), binanceApiCallback));
                        }
                );
    }

    public void showPumps() {
        Consumer<String> callback = key -> {
            System.out.println("Stopping events for " + key);
            Closeable closeable = currentPumps.get(key);
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runPump(callback);
            }
        }, 0, pumpDetectorProperties.getTimeToDetect());
    }

    public void runPump(Consumer<String> callback) {
        try {
            eventsMap.entrySet()
                    .stream().filter(k -> !k.getValue().isEmpty())
                    .forEach(k -> runPump(k.getKey(), callback));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void runPump(String symbol, Consumer<String> callback) {
        List<AggTradeEvent> trades = new ArrayList<>(eventsMap.get(symbol));
        eventsMap.get(symbol).clear();
        pumpInstantDetector.detect(symbol, trades, callback);
    }



}

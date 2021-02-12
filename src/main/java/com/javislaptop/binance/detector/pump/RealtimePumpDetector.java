package com.javislaptop.binance.detector.pump;

import com.javislaptop.binance.api.stream.BinanceDataStreamer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@EnableConfigurationProperties(PumpDetectorProperties.class)
public class RealtimePumpDetector {

    private static final Logger logger = LogManager.getLogger(RealtimePumpDetector.class);

    private final PumpDetectorProperties pumpDetectorProperties;
    private final PumpInstantDetector pumpInstantDetector;
    private final BinanceDataStreamer dataDownloader;

    public RealtimePumpDetector(PumpDetectorProperties pumpDetectorProperties, PumpInstantDetector pumpInstantDetector, BinanceDataStreamer dataDownloader) {
        this.pumpDetectorProperties = pumpDetectorProperties;
        this.pumpInstantDetector = pumpInstantDetector;
        this.dataDownloader = dataDownloader;
    }

    public void enablePumpDetection() {
        pumpDetectorProperties.getSymbols().forEach(
                symbol -> {
                    dataDownloader.enableAggTradeEvents(symbol);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            logger.debug("Detecting pumps for {}", symbol);
                            pumpInstantDetector.detect(symbol);
                        }
                    }, 0, pumpDetectorProperties.getTimeToDetect());
                }
        );
    }

}

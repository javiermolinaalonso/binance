package com.javislaptop.binance.detector.pump;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "pump-detector")
public class PumpDetectorProperties {

    private List<String> symbols;

    private Integer minTrades;
    private Integer minBuys;
    private Double maxBuyerRatio;
    private Long timeToDetect;

    public List<String> getSymbols() {
        return symbols;
    }

    public PumpDetectorProperties setSymbols(List<String> symbols) {
        this.symbols = symbols;
        return this;
    }

    public Integer getMinTrades() {
        return minTrades;
    }

    public PumpDetectorProperties setMinTrades(Integer minTrades) {
        this.minTrades = minTrades;
        return this;
    }

    public Integer getMinBuys() {
        return minBuys;
    }

    public PumpDetectorProperties setMinBuys(Integer minBuys) {
        this.minBuys = minBuys;
        return this;
    }

    public Double getMaxBuyerRatio() {
        return maxBuyerRatio;
    }

    public PumpDetectorProperties setMaxBuyerRatio(Double maxBuyerRatio) {
        this.maxBuyerRatio = maxBuyerRatio;
        return this;
    }

    public Long getTimeToDetect() {
        return timeToDetect;
    }

    public void setTimeToDetect(Long timeToDetect) {
        this.timeToDetect = timeToDetect;
    }
}

package com.javislaptop.binance.detector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "pump-detector")
public class PumpDetectorProperties {

    private List<String> symbols;

    private Integer minTrades;
    private Integer minBuys;
    private Integer minIncrease;
    private Double maxMakerRatio;
    private Double volumeRatio;
    private Double averageAskDistance;
    private boolean orderBookEnabled;
    private boolean volumeEnabled;
    private Long timeToDetect;
    private Integer minSells;
    private double minDecrease;

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

    public Integer getMinIncrease() {
        return minIncrease;
    }

    public PumpDetectorProperties setMinIncrease(Integer minIncrease) {
        this.minIncrease = minIncrease;
        return this;
    }

    public Double getMaxMakerRatio() {
        return maxMakerRatio;
    }

    public PumpDetectorProperties setMaxMakerRatio(Double maxMakerRatio) {
        this.maxMakerRatio = maxMakerRatio;
        return this;
    }

    public Double getVolumeRatio() {
        return volumeRatio;
    }

    public PumpDetectorProperties setVolumeRatio(Double volumeRatio) {
        this.volumeRatio = volumeRatio;
        return this;
    }

    public Double getAverageAskDistance() {
        return averageAskDistance;
    }

    public PumpDetectorProperties setAverageAskDistance(Double averageAskDistance) {
        this.averageAskDistance = averageAskDistance;
        return this;
    }

    public boolean isOrderBookEnabled() {
        return orderBookEnabled;
    }

    public void setOrderBookEnabled(boolean orderBookEnabled) {
        this.orderBookEnabled = orderBookEnabled;
    }

    public boolean isVolumeEnabled() {
        return volumeEnabled;
    }

    public void setVolumeEnabled(boolean volumeEnabled) {
        this.volumeEnabled = volumeEnabled;
    }

    public Long getTimeToDetect() {
        return timeToDetect;
    }

    public void setTimeToDetect(Long timeToDetect) {
        this.timeToDetect = timeToDetect;
    }

    public Integer getMinSells() {
        return minSells;
    }

    public void setMinSells(Integer minSells) {
        this.minSells = minSells;
    }

    public double getMinDecrease() {
        return minDecrease;
    }

    public void setMinDecrease(double minDecrease) {
        this.minDecrease = minDecrease;
    }
}

package com.javislaptop.binance.strategy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "pump-strategy.quick")
public class PurchaseAndSellQuickStrategyProperties {

    private long timeout;
    private String benefitPercent;
    private String lossPercent;

    public String getBenefitPercent() {
        return benefitPercent;
    }

    public void setBenefitPercent(String benefitPercent) {
        this.benefitPercent = benefitPercent;
    }

    public String getLossPercent() {
        return lossPercent;
    }

    public void setLossPercent(String lossPercent) {
        this.lossPercent = lossPercent;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

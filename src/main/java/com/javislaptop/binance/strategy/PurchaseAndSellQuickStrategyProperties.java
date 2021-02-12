package com.javislaptop.binance.strategy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "pump-strategy.quick")
public class PurchaseAndSellQuickStrategyProperties {

    private long timeout;
    private String benefitPercent;
    private String lossPercent;
    private String purchaseAmount;

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

    public String getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(String purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }
}

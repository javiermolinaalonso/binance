package com.javislaptop.binance.detector.martingala;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ConfigurationProperties(prefix = "martingala")
public class MartingalaProperties {

    private String baseAmount;
    private String decrease;
    private String originalAmount;
    private String baseCurrency;
    private List<String> tradingCurrency;
    private String from;
    private String to;
    private String comission;
    private String interval;
    private boolean automaticThreshold;
    private boolean limitToInitialInvestment;
    private int minutesToBuyAgain;

    public BigDecimal getBaseAmount() {
        return new BigDecimal(baseAmount);
    }

    public void setBaseAmount(String baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getDecrease() {
        return new BigDecimal(decrease);
    }

    public void setDecrease(String decrease) {
        this.decrease = decrease;
    }

    public BigDecimal getOriginalAmount() {
        return new BigDecimal(originalAmount);
    }

    public void setOriginalAmount(String originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public List<String> getTradingCurrency() {
        return tradingCurrency;
    }

    public void setTradingCurrency(List<String> tradingCurrency) {
        this.tradingCurrency = tradingCurrency;
    }

    public LocalDate getFrom() {
        return LocalDate.parse(from);
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return LocalDate.parse(to);
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigDecimal getComission() {
        return new BigDecimal(comission);
    }

    public void setComission(String comission) {
        this.comission = comission;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public boolean isAutomaticThreshold() {
        return automaticThreshold;
    }

    public void setAutomaticThreshold(boolean automaticThreshold) {
        this.automaticThreshold = automaticThreshold;
    }

    public boolean isLimitToInitialInvestment() {
        return limitToInitialInvestment;
    }

    public void setLimitToInitialInvestment(boolean limitToInitialInvestment) {
        this.limitToInitialInvestment = limitToInitialInvestment;
    }

    public int getMinutesToBuyAgain() {
        return minutesToBuyAgain;
    }

    public void setMinutesToBuyAgain(int minutesToBuyAgain) {
        this.minutesToBuyAgain = minutesToBuyAgain;
    }
}

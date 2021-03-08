package com.javislaptop.binance.detector.martingala;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ConfigurationProperties(prefix = "martingala")
public class MartingalaProperties {

    private String baseAmount;
    private String decrease;
    private String increase;
    private String originalAmount;
    private String baseCurrency;
    private List<String> tradingCurrency;
    private String from;
    private String to;
    private String comission;
    private boolean purchaseBeginningDay;

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

    public BigDecimal getIncrease() {
        return new BigDecimal(increase);
    }

    public void setIncrease(String increase) {
        this.increase = increase;
    }

    public boolean isPurchaseBeginningDay() {
        return purchaseBeginningDay;
    }

    public void setPurchaseBeginningDay(boolean purchaseBeginningDay) {
        this.purchaseBeginningDay = purchaseBeginningDay;
    }
}

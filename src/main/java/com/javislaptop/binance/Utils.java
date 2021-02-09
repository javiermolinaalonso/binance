package com.javislaptop.binance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    public static BigDecimal calculateBenefit(BigDecimal origin, BigDecimal end) {
        return end.subtract(origin).divide(origin, 8, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));
    }
}

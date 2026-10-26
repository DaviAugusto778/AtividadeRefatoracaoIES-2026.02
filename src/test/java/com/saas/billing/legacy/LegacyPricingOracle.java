package com.saas.billing.legacy;

import java.time.LocalDate;


public final class LegacyPricingOracle {

    private LegacyPricingOracle() {
    }

    public static double legacyPrice(double basePrice, LocalDate signupDate, LocalDate today,
                                     int overdueDays, String promoCode, boolean isNonProfit) {

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15); // 15% após 2 anos
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10); // 10% após 1 ano
        }

        if (isNonProfit) {
            calculatedPrice = calculatedPrice * 0.70; // 30% off para ONGs
        }

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (overdueDays > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * overdueDays;
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }

        return calculatedPrice;
    }
}

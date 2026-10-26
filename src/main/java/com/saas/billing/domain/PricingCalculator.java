package com.saas.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;


public final class PricingCalculator {

    static final BigDecimal LOYALTY_24_MONTHS_RATE = new BigDecimal("0.15");
    static final BigDecimal LOYALTY_12_MONTHS_RATE = new BigDecimal("0.10");
    static final BigDecimal NON_PROFIT_RATE = new BigDecimal("0.30");
    static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    static final BigDecimal WELCOME_RATE = new BigDecimal("0.10");
    static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    private static final String BLACK_FRIDAY = "BLACKFRIDAY";
    private static final String WELCOME10 = "WELCOME10";

    public Money calculate(BillingCommand command, LocalDate today) {
        int monthsActive = monthsActive(command.signupDate(), today);
        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command, today, monthsActive);
        price = applyOverdueCharges(price, command.overdueDays());
        return price.atLeastZero().roundedToCents();
    }

    static int monthsActive(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    private static Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > 24) {
            return basePrice.discountedBy(LOYALTY_24_MONTHS_RATE);
        }
        if (monthsActive > 12) {
            return basePrice.discountedBy(LOYALTY_12_MONTHS_RATE);
        }
        return basePrice;
    }

    private static Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.discountedBy(NON_PROFIT_RATE) : price;
    }

    private static Money applyPromoCode(Money price, BillingCommand command, LocalDate today, int monthsActive) {
        if (!command.hasPromoCode()) {
            return price;
        }
        String code = command.promoCode();
        if (code.equalsIgnoreCase(BLACK_FRIDAY) && today.getMonthValue() == 11) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (code.equalsIgnoreCase(WELCOME10) && monthsActive <= 1) {
            return price.discountedBy(WELCOME_RATE);
        }
        return price;
    }

    private static Money applyOverdueCharges(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times(LATE_PENALTY_RATE);
        Money interest = price.times(DAILY_INTEREST_RATE).times(overdueDays);
        return price.plus(penalty).plus(interest);
    }
}

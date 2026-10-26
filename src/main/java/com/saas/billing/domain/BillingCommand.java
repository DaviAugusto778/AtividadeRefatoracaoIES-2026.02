package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;


public record BillingCommand(
        String customerId,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId é obrigatório");
        Objects.requireNonNull(basePrice, "basePrice é obrigatório");
        Objects.requireNonNull(signupDate, "signupDate é obrigatório");
    }
    
    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }
}

package com.saas.billing.domain;

public enum InvoiceStatus {
    PAID,
    FAILED;

    public static InvoiceStatus from(PaymentResult payment) {
        return payment.isApproved() ? PAID : FAILED;
    }
}

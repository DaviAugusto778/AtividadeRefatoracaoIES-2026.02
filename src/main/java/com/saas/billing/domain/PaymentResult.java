package com.saas.billing.domain;

public enum PaymentResult {
    APPROVED,
    DECLINED;

    public boolean isApproved() {
        return this == APPROVED;
    }
}

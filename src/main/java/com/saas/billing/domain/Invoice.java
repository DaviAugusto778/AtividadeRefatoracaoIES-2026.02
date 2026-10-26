package com.saas.billing.domain;

import java.util.Objects;

public record Invoice(String customerId, Money amount, InvoiceStatus status) {

    public Invoice {
        Objects.requireNonNull(customerId, "customerId é obrigatório");
        Objects.requireNonNull(amount, "amount é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
    }
}

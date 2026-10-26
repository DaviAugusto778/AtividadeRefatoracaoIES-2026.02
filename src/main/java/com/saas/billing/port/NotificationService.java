package com.saas.billing.port;

import com.saas.billing.domain.Money;

public interface NotificationService {

    /**
     * @throws NotificationException se o envio falhar
     */
    void sendInvoiceReceipt(String email, Money amount);
}

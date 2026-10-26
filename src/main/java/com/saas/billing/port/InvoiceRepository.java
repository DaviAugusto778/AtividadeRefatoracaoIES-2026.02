package com.saas.billing.port;

import com.saas.billing.domain.Invoice;

public interface InvoiceRepository {

    /**
     * @throws InfrastructureException se o armazenamento estiver indisponível
     */
    void save(Invoice invoice);
}

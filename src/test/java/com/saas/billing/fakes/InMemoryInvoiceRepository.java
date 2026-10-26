package com.saas.billing.fakes;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.InvoiceRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryInvoiceRepository implements InvoiceRepository {

    private final List<Invoice> saved = new ArrayList<>();

    @Override
    public void save(Invoice invoice) {
        saved.add(invoice);
    }

    public List<Invoice> saved() {
        return saved;
    }

    public Invoice last() {
        return saved.get(saved.size() - 1);
    }
}

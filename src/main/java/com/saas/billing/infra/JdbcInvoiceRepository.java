package com.saas.billing.infra;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.InfrastructureException;
import com.saas.billing.port.InvoiceRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcInvoiceRepository implements InvoiceRepository {

    private static final String SQL_INSERT =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    private final DatabaseConfig database;

    public JdbcInvoiceRepository(DatabaseConfig database) {
        this.database = database;
    }

    @Override
    public void save(Invoice invoice) {
        try (Connection conn = database.openConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setString(1, invoice.customerId());
            stmt.setBigDecimal(2, invoice.amount().amount());
            stmt.setString(3, invoice.status().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException("Falha ao gravar a fatura do cliente " + invoice.customerId(), e);
        }
    }
}

package com.saas.billing.infra;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InfrastructureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcCustomerRepository implements CustomerRepository {

    private static final String SQL_FIND_BY_ID = "SELECT email, status FROM customers WHERE id = ?";

    private final DatabaseConfig database;

    public JdbcCustomerRepository(DatabaseConfig database) {
        this.database = database;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        try (Connection conn = database.openConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(customerId, rs.getString("email"), rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Falha ao consultar o cliente " + customerId, e);
        }
    }
}

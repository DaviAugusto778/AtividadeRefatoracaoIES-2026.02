package com.saas.billing.fakes;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InfrastructureException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new HashMap<>();
    private boolean unavailable;

    public InMemoryCustomerRepository with(Customer customer) {
        customers.put(customer.id(), customer);
        return this;
    }

    public void simulateOutage() {
        unavailable = true;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        if (unavailable) {
            throw new InfrastructureException("banco fora do ar", new RuntimeException("connection refused"));
        }
        return Optional.ofNullable(customers.get(customerId));
    }
}

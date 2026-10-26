package com.saas.billing.port;

import com.saas.billing.domain.Customer;
import java.util.Optional;

public interface CustomerRepository {

    /**
     * @throws InfrastructureException se o armazenamento estiver indisponível
     */
    Optional<Customer> findById(String customerId);
}

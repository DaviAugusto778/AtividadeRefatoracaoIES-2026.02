package com.saas.billing.port;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;

public interface PaymentGateway {

    /**
     * Uma recusa do cartão é um resultado de negócio ({@link PaymentResult#DECLINED}), não uma exceção.
     *
     * @throws InfrastructureException se o provedor estiver indisponível
     */
    PaymentResult charge(String cardToken, Money amount);
}

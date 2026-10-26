package com.saas.billing.infra;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.port.PaymentGateway;


public class TokenPrefixPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public PaymentResult charge(String cardToken, Money amount) {
        boolean valid = cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
        return valid ? PaymentResult.APPROVED : PaymentResult.DECLINED;
    }
}

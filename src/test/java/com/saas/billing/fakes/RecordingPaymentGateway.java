package com.saas.billing.fakes;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.port.PaymentGateway;
import java.util.ArrayList;
import java.util.List;

/** Gateway falso com resultado programável; registra os valores cobrados. */
public class RecordingPaymentGateway implements PaymentGateway {

    private final PaymentResult result;
    private final List<Money> charges = new ArrayList<>();

    public RecordingPaymentGateway(PaymentResult result) {
        this.result = result;
    }

    @Override
    public PaymentResult charge(String cardToken, Money amount) {
        charges.add(amount);
        return result;
    }

    public List<Money> charges() {
        return charges;
    }
}

package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class SubscriptionBillingService {

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentGateway payments;
    private final NotificationService notifications;
    private final Clock clock;
    private final PricingCalculator pricing = new PricingCalculator();

    public SubscriptionBillingService(CustomerRepository customers, InvoiceRepository invoices,
                                      PaymentGateway payments, NotificationService notifications,
                                      Clock clock) {
        this.customers = Objects.requireNonNull(customers);
        this.invoices = Objects.requireNonNull(invoices);
        this.payments = Objects.requireNonNull(payments);
        this.notifications = Objects.requireNonNull(notifications);
        this.clock = Objects.requireNonNull(clock);
    }

    
    @Deprecated(forRemoval = true)
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
                                             String signupDateStr, int overdueDays, String promoCode,
                                             boolean isNonProfit, String paymentCardToken) {
        return processMonthlyBilling(new BillingCommand(customerId, Money.of(basePrice),
                LocalDate.parse(signupDateStr), overdueDays, promoCode, isNonProfit, paymentCardToken));
    }

    /**
     * @throws com.saas.billing.port.InfrastructureException se banco ou gateway falharem;
     *         a falha não é mais convertida em uma linha de log silenciosa.
     */
    public List<String> processMonthlyBilling(BillingCommand command) {
        List<String> logs = new ArrayList<>();
        Optional<Customer> found = customers.findById(command.customerId());
        if (found.isEmpty()) {
            return logs;
        }
        Customer customer = found.get();
        if (!customer.isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return logs;
        }
        Money amount = pricing.calculate(command, LocalDate.now(clock));
        PaymentResult payment = payments.charge(command.paymentCardToken(), amount);
        logs.add(paymentLog(command.customerId(), payment, amount));
        invoices.save(new Invoice(command.customerId(), amount, InvoiceStatus.from(payment)));
        logs.add(sendReceipt(customer, amount));
        return logs;
    }

    private static String paymentLog(String customerId, PaymentResult payment, Money amount) {
        return payment.isApproved()
                ? "Pagamento processado com sucesso: R$ " + amount
                : "Falha no pagamento do cartão para: " + customerId;
    }

    private String sendReceipt(Customer customer, Money amount) {
        try {
            notifications.sendInvoiceReceipt(customer.email(), amount);
            return "E-mail disparado para: " + customer.email();
        } catch (NotificationException e) {
            return "Falha ao enviar e-mail para: " + customer.email() + " (" + e.getMessage() + ")";
        }
    }
}

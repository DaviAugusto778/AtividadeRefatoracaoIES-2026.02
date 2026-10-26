package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private static final BigDecimal LOYALTY_24_MONTHS_RATE = new BigDecimal("0.15");
    private static final BigDecimal LOYALTY_12_MONTHS_RATE = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_RATE = new BigDecimal("0.30");
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    private static final BigDecimal WELCOME_RATE = new BigDecimal("0.10");
    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentGateway payments;
    private final NotificationService notifications;
    private final Clock clock;

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

        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        Money basePrice = command.basePrice();
        Money calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = basePrice.discountedBy(LOYALTY_24_MONTHS_RATE);
        } else if (monthsActive > 12) {
            calculatedPrice = basePrice.discountedBy(LOYALTY_12_MONTHS_RATE);
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.discountedBy(NON_PROFIT_RATE);
        }

        if (command.hasPromoCode()) {
            String promoCode = command.promoCode();
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice.minus(BLACK_FRIDAY_DISCOUNT);
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice.discountedBy(WELCOME_RATE);
            }
        }

        if (command.overdueDays() > 0) {
            Money penalty = calculatedPrice.times(LATE_PENALTY_RATE);
            Money dailyInterest = calculatedPrice.times(DAILY_INTEREST_RATE).times(command.overdueDays());
            calculatedPrice = calculatedPrice.plus(penalty).plus(dailyInterest);
        }

        Money finalPrice = calculatedPrice.atLeastZero().roundedToCents();

        Optional<Customer> found = customers.findById(command.customerId());
        if (found.isEmpty()) {
            return logs;
        }
        Customer customer = found.get();

        if (!customer.isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return logs;
        }

        PaymentResult payment = payments.charge(command.paymentCardToken(), finalPrice);
        if (payment.isApproved()) {
            logs.add("Pagamento processado com sucesso: R$ " + finalPrice);
        } else {
            logs.add("Falha no pagamento do cartão para: " + command.customerId());
        }

        invoices.save(new Invoice(command.customerId(), finalPrice, InvoiceStatus.from(payment)));

        try {
            notifications.sendInvoiceReceipt(customer.email(), finalPrice);
            logs.add("E-mail disparado para: " + customer.email());
        } catch (NotificationException e) {
            logs.add("Falha ao enviar e-mail para: " + customer.email() + " (" + e.getMessage() + ")");
        }

        return logs;
    }
}

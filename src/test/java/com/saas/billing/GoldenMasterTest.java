package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.fakes.InMemoryCustomerRepository;
import com.saas.billing.fakes.InMemoryInvoiceRepository;
import com.saas.billing.fakes.RecordingNotificationService;
import com.saas.billing.infra.TokenPrefixPaymentGateway;
import com.saas.billing.legacy.LegacyPricingOracle;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


class GoldenMasterTest {

    private static final double HALF_CENT = 0.005 + 1e-9;

    private static final double[] BASE_PRICES = {0.0, 9.9, 19.99, 30.0, 49.9, 99.9, 100.0, 149.99, 1234.56};
    private static final int[] MONTHS_SINCE_SIGNUP = {-1, 0, 1, 2, 12, 13, 24, 25, 60};
    private static final LocalDate[] TODAYS = {
            LocalDate.of(2026, 10, 26),  // fora de novembro
            LocalDate.of(2026, 11, 27),  // Black Friday
            LocalDate.of(2027, 1, 5)     // virada de ano no cálculo de meses
    };
    private static final int[] OVERDUE_DAYS = {-3, 0, 1, 5, 30};
    private static final String[] PROMO_CODES =
            {null, "", "   ", "BLACKFRIDAY", "blackfriday", "WELCOME10", "Welcome10", "INVALIDO"};
    private static final String[] TOKENS = {"tok_visa", null, "card_123"};

    @Test
    void fluxoRefatoradoReproduzOCalculoELegadoEmTodasAsCombinacoes() {
        List<String> divergences = new ArrayList<>();
        int cases = 0;

        for (LocalDate today : TODAYS) {
            for (double base : BASE_PRICES) {
                for (int months : MONTHS_SINCE_SIGNUP) {
                    for (int overdue : OVERDUE_DAYS) {
                        for (String promo : PROMO_CODES) {
                            for (boolean nonProfit : new boolean[] {false, true}) {
                                for (String token : TOKENS) {
                                    cases++;
                                    LocalDate signup = today.minusMonths(months);
                                    String divergence = compare(today, base, signup, overdue, promo, nonProfit, token);
                                    if (divergence != null) {
                                        divergences.add(divergence);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(cases > 20_000, "cobertura combinatória insuficiente: " + cases);
        assertTrue(divergences.isEmpty(),
                divergences.size() + " divergências, ex.: " + divergences.subList(0, Math.min(5, divergences.size())));
    }

    private static String compare(LocalDate today, double base, LocalDate signup, int overdue,
                                  String promo, boolean nonProfit, String token) {
        double expected = LegacyPricingOracle.legacyPrice(base, signup, today, overdue, promo, nonProfit);

        InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
        SubscriptionBillingService service = new SubscriptionBillingService(
                new InMemoryCustomerRepository().with(new Customer("c-1", "a@b.com", "ACTIVE")),
                invoices,
                new TokenPrefixPaymentGateway(),
                new RecordingNotificationService(),
                Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        service.processMonthlyBilling(new BillingCommand("c-1", Money.of(base), signup, overdue, promo, nonProfit, token));

        Invoice invoice = invoices.last();
        boolean legacyPaid = token != null && token.startsWith("tok_");
        InvoiceStatus expectedStatus = legacyPaid ? InvoiceStatus.PAID : InvoiceStatus.FAILED;
        double actual = invoice.amount().amount().doubleValue();

        if (Math.abs(actual - expected) > HALF_CENT || invoice.status() != expectedStatus) {
            return String.format("today=%s base=%s signup=%s overdue=%d promo=%s ong=%s token=%s → legado=%s/%s novo=%s/%s",
                    today, base, signup, overdue, promo, nonProfit, token,
                    expected, expectedStatus, invoice.amount(), invoice.status());
        }
        return null;
    }
}

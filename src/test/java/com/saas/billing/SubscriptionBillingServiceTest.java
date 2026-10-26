package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.fakes.InMemoryCustomerRepository;
import com.saas.billing.fakes.InMemoryInvoiceRepository;
import com.saas.billing.fakes.RecordingNotificationService;
import com.saas.billing.fakes.RecordingPaymentGateway;
import com.saas.billing.port.InfrastructureException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;


class SubscriptionBillingServiceTest {

    private static final LocalDate OCT_26 = LocalDate.of(2026, 10, 26);
    private static final LocalDate NOV_27 = LocalDate.of(2026, 11, 27);

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository()
            .with(new Customer("c-1", "ana@ex.com", "ACTIVE"))
            .with(new Customer("c-2", "bia@ex.com", "SUSPENDED"))
            .with(new Customer("c-3", "caio@ex.com", "active"));
    private final InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
    private final RecordingNotificationService notifications = new RecordingNotificationService();

    private SubscriptionBillingService service(PaymentResult paymentResult, LocalDate today) {
        return service(new RecordingPaymentGateway(paymentResult), today);
    }

    private SubscriptionBillingService service(RecordingPaymentGateway gateway, LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new SubscriptionBillingService(customers, invoices, gateway, notifications, clock);
    }

    private static BillingCommand command(String customerId, String base, LocalDate signup) {
        return new BillingCommand(customerId, Money.of(base), signup, 0, null, false, "tok_visa");
    }

    @Test
    void cobrancaAprovadaGeraFaturaPagaENotifica() {
        RecordingPaymentGateway gateway = new RecordingPaymentGateway(PaymentResult.APPROVED);

        List<String> logs = service(gateway, OCT_26).processMonthlyBilling(command("c-1", "100.00", OCT_26));

        assertEquals(List.of("Pagamento processado com sucesso: R$ 100.00", "E-mail disparado para: ana@ex.com"), logs);
        assertEquals(List.of(Money.of("100.00")), gateway.charges());
        assertEquals(InvoiceStatus.PAID, invoices.last().status());
        assertEquals(Money.of("100.00"), invoices.last().amount());
        assertEquals(1, notifications.sent().size());
    }

    @Test
    void cobrancaRecusadaAindaGeraFaturaFalhaENotifica() {
        List<String> logs = service(PaymentResult.DECLINED, OCT_26).processMonthlyBilling(command("c-1", "100.00", OCT_26));

        assertEquals(List.of("Falha no pagamento do cartão para: c-1", "E-mail disparado para: ana@ex.com"), logs);
        assertEquals(InvoiceStatus.FAILED, invoices.last().status());
    }

    @Test
    void clienteInativoNaoECobrado() {
        RecordingPaymentGateway gateway = new RecordingPaymentGateway(PaymentResult.APPROVED);

        List<String> logs = service(gateway, OCT_26).processMonthlyBilling(command("c-2", "100.00", OCT_26));

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: c-2"), logs);
        assertTrue(gateway.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void statusAtivoNaoDiferenciaMaiusculas() {
        List<String> logs = service(PaymentResult.APPROVED, OCT_26).processMonthlyBilling(command("c-3", "50.00", OCT_26));

        assertEquals("Pagamento processado com sucesso: R$ 50.00", logs.get(0));
    }

    @Test
    void clienteInexistenteNaoGeraNada() {
        RecordingPaymentGateway gateway = new RecordingPaymentGateway(PaymentResult.APPROVED);

        List<String> logs = service(gateway, OCT_26).processMonthlyBilling(command("nao-existe", "100.00", OCT_26));

        assertTrue(logs.isEmpty());
        assertTrue(gateway.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
    }

    @Test
    void falhaDeBancoNaoESilenciada() {
        customers.simulateOutage();
        RecordingPaymentGateway gateway = new RecordingPaymentGateway(PaymentResult.APPROVED);
        SubscriptionBillingService service = service(gateway, OCT_26);

        assertThrows(InfrastructureException.class,
                () -> service.processMonthlyBilling(command("c-1", "100.00", OCT_26)));
        assertTrue(gateway.charges().isEmpty());
    }

    @Test
    void falhaDeEmailNaoDesfazACobranca() {
        notifications.simulateSmtpFailure();

        List<String> logs = service(PaymentResult.APPROVED, OCT_26).processMonthlyBilling(command("c-1", "100.00", OCT_26));

        assertEquals("Pagamento processado com sucesso: R$ 100.00", logs.get(0));
        assertEquals("Falha ao enviar e-mail para: ana@ex.com (SMTP indisponível)", logs.get(1));
        assertEquals(InvoiceStatus.PAID, invoices.last().status());
    }

    @Test
    void relogioInjetadoTornaABlackFridayDeterministica() {
        BillingCommand blackFriday =
                new BillingCommand("c-1", Money.of("100.00"), OCT_26, 0, "BLACKFRIDAY", false, "tok_visa");

        service(PaymentResult.APPROVED, OCT_26).processMonthlyBilling(blackFriday);
        assertEquals(Money.of("100.00"), invoices.last().amount());

        service(PaymentResult.APPROVED, NOV_27).processMonthlyBilling(blackFriday);
        assertEquals(Money.of("70.00"), invoices.last().amount());
    }

    @Test
    void ongComFidelidadeEAtrasoAcumulaRegrasNaOrdemDoLegado() {
        BillingCommand ong = new BillingCommand("c-1", Money.of("99.90"), LocalDate.of(2023, 9, 1),
                10, null, true, "tok_visa");

        service(PaymentResult.APPROVED, OCT_26).processMonthlyBilling(ong);

        assertEquals(Money.of("61.22"), invoices.last().amount());
    }

    @Test
    @SuppressWarnings("removal")
    void assinaturaLegadaContinuaFuncionando() {
        List<String> logs = service(PaymentResult.APPROVED, OCT_26).processMonthlyBilling(
                "c-1", "PRO", 100.0, "2026-10-01", 0, null, false, "tok_visa");

        assertEquals("Pagamento processado com sucesso: R$ 100.00", logs.get(0));
    }
}

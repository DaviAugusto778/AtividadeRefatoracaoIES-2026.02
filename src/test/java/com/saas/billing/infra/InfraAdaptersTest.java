package com.saas.billing.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InfraAdaptersTest {

    private final TokenPrefixPaymentGateway gateway = new TokenPrefixPaymentGateway();

    @Test
    void gatewayPreservaRegraDoTokenDoLegado() {
        assertEquals(PaymentResult.APPROVED, gateway.charge("tok_123", Money.of("10")));
        assertEquals(PaymentResult.DECLINED, gateway.charge("TOK_123", Money.of("10")));
        assertEquals(PaymentResult.DECLINED, gateway.charge("card_123", Money.of("10")));
        assertEquals(PaymentResult.DECLINED, gateway.charge(null, Money.of("10")));
    }

    @Test
    void mensagemDoReciboMantemOTextoOriginal() {
        assertEquals("Olá! Sua fatura foi processada no valor de R$ 61.22",
                SmtpNotificationService.messageFor(Money.of("61.22")));
    }

    @Test
    void credenciaisDoBancoVemDoAmbiente() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(
                Map.of("BILLING_DB_USER", "billing", "BILLING_DB_PASSWORD", "s3cr3t"));

        assertEquals(DatabaseConfig.DEFAULT_URL, config.url());
        assertEquals("billing", config.user());
    }

    @Test
    void semCredenciaisFalhaNaInicializacao() {
        assertThrows(IllegalStateException.class, () -> DatabaseConfig.fromEnvironment(Map.of()));
    }
}

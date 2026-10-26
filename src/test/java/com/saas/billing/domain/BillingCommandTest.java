package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingCommandTest {

    private static BillingCommand comCupom(String promoCode) {
        return new BillingCommand("c-1", Money.of("100.00"), LocalDate.of(2025, 1, 10),
                0, promoCode, false, "tok_abc");
    }

    @Test
    void cupomNuloOuEmBrancoNaoConta() {
        assertFalse(comCupom(null).hasPromoCode());
        assertFalse(comCupom("").hasPromoCode());
        assertFalse(comCupom("   ").hasPromoCode());
    }

    @Test
    void cupomPreenchidoConta() {
        assertTrue(comCupom("WELCOME10").hasPromoCode());
    }

    @Test
    void camposObrigatorios() {
        assertThrows(NullPointerException.class, () ->
                new BillingCommand(null, Money.of("1"), LocalDate.now(), 0, null, false, null));
        assertThrows(NullPointerException.class, () ->
                new BillingCommand("c", null, LocalDate.now(), 0, null, false, null));
        assertThrows(NullPointerException.class, () ->
                new BillingCommand("c", Money.of("1"), null, 0, null, false, null));
    }
}

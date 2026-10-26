package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void evitaErroDePontoFlutuanteDoDouble() {
        // Em double: 0.1 + 0.2 = 0.30000000000000004
        Money soma = Money.of(0.1).plus(Money.of(0.2));
        assertEquals(Money.of("0.3"), soma);
    }

    @Test
    void conversaoDeDoubleUsaRepresentacaoDecimal() {
        assertEquals(new BigDecimal("99.9"), Money.of(99.9).amount());
    }

    @Test
    void descontoPercentual() {
        assertEquals(Money.of("85.00"), Money.of("100.00").discountedBy(new BigDecimal("0.15")));
    }

    @Test
    void arredondaParaCentavosComHalfUp() {
        assertEquals(new BigDecimal("10.01"), Money.of("10.005").roundedToCents().amount());
        assertEquals(new BigDecimal("10.00"), Money.of("10.004").roundedToCents().amount());
    }

    @Test
    void pisoEmZero() {
        assertSame(Money.ZERO, Money.of("-5.00").atLeastZero());
        assertEquals(Money.of("5.00"), Money.of("5.00").atLeastZero());
    }

    @Test
    void ehImutavel() {
        Money original = Money.of("10.00");
        original.plus(Money.of("5.00"));
        original.times(3);
        assertEquals(Money.of("10.00"), original);
    }

    @Test
    void igualdadePorValorIgnoraEscala() {
        assertEquals(Money.of("10.0"), Money.of("10.00"));
        assertEquals(Money.of("10.0").hashCode(), Money.of("10.00").hashCode());
        assertNotEquals(Money.of("10.00"), Money.of("10.01"));
    }

    @Test
    void toStringSempreComDuasCasas() {
        assertEquals("85.00", Money.of(85.0).toString());
        assertEquals("62.94", Money.of("62.937").toString());
    }

    @Test
    void rejeitaValoresInvalidos() {
        assertThrows(NullPointerException.class, () -> new Money(null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Money.of(Double.POSITIVE_INFINITY));
    }

    @Test
    void detectaNegativo() {
        assertTrue(Money.of("-0.01").isNegative());
    }
}

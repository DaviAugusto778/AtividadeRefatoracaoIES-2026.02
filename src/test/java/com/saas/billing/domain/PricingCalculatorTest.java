package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private static final LocalDate OCT_26 = LocalDate.of(2026, 10, 26);
    private static final LocalDate NOV_27 = LocalDate.of(2026, 11, 27);

    private final PricingCalculator calculator = new PricingCalculator();

    private Money price(String base, LocalDate signup, LocalDate today, int overdue, String promo, boolean ong) {
        return calculator.calculate(new BillingCommand("c-1", Money.of(base), signup, overdue, promo, ong, "tok_x"), today);
    }

    private Money price(String base, int monthsActive, LocalDate today) {
        return price(base, today.minusMonths(monthsActive), today, 0, null, false);
    }

    @Nested
    class Fidelidade {

        @ParameterizedTest(name = "{0} meses → R$ {1}")
        @CsvSource({"0, 100.00", "12, 100.00", "13, 90.00", "24, 90.00", "25, 85.00", "120, 85.00"})
        void descontoPorTempoDeCasa(int months, String expected) {
            assertEquals(Money.of(expected), price("100.00", months, OCT_26));
        }

        @Test
        void mesesIgnoramODiaDoMes() {
            assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)));
            assertEquals(13, PricingCalculator.monthsActive(LocalDate.of(2025, 12, 31), LocalDate.of(2027, 1, 1)));
        }
    }

    @Nested
    class Ong {

        @Test
        void trintaPorCentoDeDesconto() {
            assertEquals(Money.of("70.00"), price("100.00", OCT_26, OCT_26, 0, null, true));
        }

        @Test
        void acumulaComFidelidade() {
            assertEquals(Money.of("59.50"), price("100.00", OCT_26.minusMonths(30), OCT_26, 0, null, true));
        }
    }

    @Nested
    class Cupons {

        @Test
        void blackFridaySoValeEmNovembro() {
            assertEquals(Money.of("70.00"), price("100.00", NOV_27, NOV_27, 0, "BLACKFRIDAY", false));
            assertEquals(Money.of("100.00"), price("100.00", OCT_26, OCT_26, 0, "BLACKFRIDAY", false));
        }

        @Test
        void blackFridayNaoGeraValorNegativo() {
            assertEquals(Money.ZERO, price("19.90", NOV_27, NOV_27, 0, "blackfriday", false));
        }

        @Test
        void welcome10SoAteOPrimeiroMes() {
            assertEquals(Money.of("90.00"), price("100.00", OCT_26.minusMonths(1), OCT_26, 0, "welcome10", false));
            assertEquals(Money.of("100.00"), price("100.00", OCT_26.minusMonths(2), OCT_26, 0, "WELCOME10", false));
        }

        @Test
        void cupomDesconhecidoOuEmBrancoNaoAltera() {
            assertEquals(Money.of("100.00"), price("100.00", OCT_26, OCT_26, 0, "CUPOM_FALSO", false));
            assertEquals(Money.of("100.00"), price("100.00", OCT_26, OCT_26, 0, "  ", false));
        }
    }

    @Nested
    class Atraso {

        @Test
        void multaDeDoisPorCentoMaisJurosDiarios() {
            // 100 + 2,00 multa + 0,10 x 5 dias = 102,50
            assertEquals(Money.of("102.50"), price("100.00", OCT_26, OCT_26, 5, null, false));
        }

        @Test
        void diasNegativosOuZeroNaoGeramEncargos() {
            assertEquals(Money.of("100.00"), price("100.00", OCT_26, OCT_26, 0, null, false));
            assertEquals(Money.of("100.00"), price("100.00", OCT_26, OCT_26, -4, null, false));
        }

        @Test
        void encargosIncidemSobreOValorJaDescontado() {
            assertEquals(Money.of("72.10"), price("100.00", NOV_27, NOV_27, 10, "BLACKFRIDAY", false));
        }
    }

    @Test
    void resultadoSempreComDuasCasasDecimais() {
        assertEquals(2, price("99.90", OCT_26.minusMonths(37), OCT_26, 10, null, true).amount().scale());
    }
}

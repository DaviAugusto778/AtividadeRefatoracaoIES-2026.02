package com.saas.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int CENTS_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Valor monetário inválido: " + amount);
        }
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public Money times(long factor) {
        return times(BigDecimal.valueOf(factor));
    }

    public Money discountedBy(BigDecimal rate) {
        return times(BigDecimal.ONE.subtract(rate));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money atLeastZero() {
        return isNegative() ? ZERO : this;
    }

    public Money roundedToCents() {
        return new Money(amount.setScale(CENTS_SCALE, ROUNDING));
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.setScale(CENTS_SCALE, ROUNDING).toPlainString();
    }
}

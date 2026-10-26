package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.mail.SimpleEmail;

public class SubscriptionBillingService {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/saas_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";

    private static final BigDecimal LOYALTY_24_MONTHS_RATE = new BigDecimal("0.15");
    private static final BigDecimal LOYALTY_12_MONTHS_RATE = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_RATE = new BigDecimal("0.30");
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    private static final BigDecimal WELCOME_RATE = new BigDecimal("0.10");
    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    @Deprecated(forRemoval = true)
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
                                             String signupDateStr, int overdueDays, String promoCode,
                                             boolean isNonProfit, String paymentCardToken) {
        return processMonthlyBilling(new BillingCommand(customerId, Money.of(basePrice),
                LocalDate.parse(signupDateStr), overdueDays, promoCode, isNonProfit, paymentCardToken));
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now();

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

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sqlCustomer = "SELECT email, status FROM customers WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sqlCustomer);
            stmt.setString(1, command.customerId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String status = rs.getString("status");

                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                    return logs;
                }

                String paymentCardToken = command.paymentCardToken();
                boolean paymentSuccess = false;
                if (paymentCardToken != null && paymentCardToken.startsWith("tok_")) {
                    paymentSuccess = true;
                    logs.add("Pagamento processado com sucesso: R$ " + finalPrice);
                } else {
                    logs.add("Falha no pagamento do cartão para: " + command.customerId());
                }

                String sqlInvoice = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
                PreparedStatement stmtInvoice = conn.prepareStatement(sqlInvoice);
                stmtInvoice.setString(1, command.customerId());
                stmtInvoice.setBigDecimal(2, finalPrice.amount());
                stmtInvoice.setString(3, paymentSuccess ? "PAID" : "FAILED");
                stmtInvoice.executeUpdate();

                SimpleEmail emailSender = new SimpleEmail();
                emailSender.setHostName("smtp.empresa.com");
                emailSender.setSmtpPort(587);
                emailSender.setFrom("billing@empresa.com");
                emailSender.setSubject("Sua Fatura Mensal");
                emailSender.setMsg("Olá! Sua fatura foi processada no valor de R$ " + finalPrice);
                emailSender.addTo(email);

                logs.add("E-mail disparado para: " + email);
            }
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}

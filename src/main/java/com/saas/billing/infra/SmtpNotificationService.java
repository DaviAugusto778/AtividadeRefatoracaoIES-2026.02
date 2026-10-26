package com.saas.billing.infra;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationException;
import com.saas.billing.port.NotificationService;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class SmtpNotificationService implements NotificationService {

    static final String SUBJECT = "Sua Fatura Mensal";
    static final String BODY_PREFIX = "Olá! Sua fatura foi processada no valor de R$ ";

    private final String host;
    private final int port;
    private final String from;

    public SmtpNotificationService(String host, int port, String from) {
        this.host = host;
        this.port = port;
        this.from = from;
    }

    public static SmtpNotificationService fromEnvironment() {
        return new SmtpNotificationService(
                System.getenv().getOrDefault("BILLING_SMTP_HOST", "smtp.empresa.com"),
                Integer.parseInt(System.getenv().getOrDefault("BILLING_SMTP_PORT", "587")),
                System.getenv().getOrDefault("BILLING_SMTP_FROM", "billing@empresa.com"));
    }

    static String messageFor(Money amount) {
        return BODY_PREFIX + amount;
    }

    @Override
    public void sendInvoiceReceipt(String email, Money amount) {
        try {
            SimpleEmail message = new SimpleEmail();
            message.setHostName(host);
            message.setSmtpPort(port);
            message.setFrom(from);
            message.setSubject(SUBJECT);
            message.setMsg(messageFor(amount));
            message.addTo(email);
            message.send();
        } catch (EmailException e) {
            throw new NotificationException("Falha ao enviar recibo para " + email, e);
        }
    }
}

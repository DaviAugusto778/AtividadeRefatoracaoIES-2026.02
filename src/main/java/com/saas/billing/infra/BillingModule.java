package com.saas.billing.infra;

import com.saas.billing.SubscriptionBillingService;
import java.time.Clock;


public final class BillingModule {

    private BillingModule() {
    }

    public static SubscriptionBillingService createDefault() {
        DatabaseConfig database = DatabaseConfig.fromEnvironment();
        return new SubscriptionBillingService(
                new JdbcCustomerRepository(database),
                new JdbcInvoiceRepository(database),
                new TokenPrefixPaymentGateway(),
                SmtpNotificationService.fromEnvironment(),
                Clock.systemDefaultZone());
    }
}

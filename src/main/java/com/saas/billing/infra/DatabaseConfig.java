package com.saas.billing.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;


public record DatabaseConfig(String url, String user, String password) {

    static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/saas_db";

    public DatabaseConfig {
        Objects.requireNonNull(url, "url é obrigatória");
    }

    public static DatabaseConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DatabaseConfig fromEnvironment(Map<String, String> env) {
        return new DatabaseConfig(
                env.getOrDefault("BILLING_DB_URL", DEFAULT_URL),
                required(env, "BILLING_DB_USER"),
                required(env, "BILLING_DB_PASSWORD"));
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não definida: " + key);
        }
        return value;
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}

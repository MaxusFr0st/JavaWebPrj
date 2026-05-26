package hr.algebra.javawebprj.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Railway database env vars to {@code spring.datasource.*} before auto-configuration.
 * Supports {@code DATABASE_URL} or {@code PGHOST}/{@code PGPORT}/… variables.
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "railwayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasJdbcUrl(environment)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (isUsablePostgresUrl(databaseUrl)) {
            applyFromUrl(properties, databaseUrl.trim());
        } else {
            applyFromPgVars(environment, properties);
        }

        if (!properties.containsKey("spring.datasource.url")) {
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, properties));
    }

    private static boolean hasJdbcUrl(ConfigurableEnvironment environment) {
        return hasText(environment.getProperty("spring.datasource.url"))
                || hasText(environment.getProperty("spring.datasource.jdbc-url"));
    }

    private static boolean isUsablePostgresUrl(String databaseUrl) {
        if (!hasText(databaseUrl)) {
            return false;
        }
        String trimmed = databaseUrl.trim();
        return trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://");
    }

    private static void applyFromUrl(Map<String, Object> properties, String databaseUrl) {
        ParsedPostgres parsed = parsePostgresUrl(databaseUrl);
        properties.put("spring.datasource.url", parsed.jdbcUrl());
        if (parsed.username() != null) {
            properties.put("spring.datasource.username", parsed.username());
        }
        if (parsed.password() != null) {
            properties.put("spring.datasource.password", parsed.password());
        }
    }

    private static void applyFromPgVars(ConfigurableEnvironment environment, Map<String, Object> properties) {
        String host = environment.getProperty("PGHOST");
        if (!hasText(host)) {
            return;
        }
        String port = environment.getProperty("PGPORT", "5432");
        String database = environment.getProperty("PGDATABASE", "railway");
        String username = environment.getProperty("PGUSER");
        String password = environment.getProperty("PGPASSWORD");

        String jdbcQuery = host.contains("railway.internal") ? "" : "?sslmode=require";
        properties.put("spring.datasource.url",
                "jdbc:postgresql://" + host + ":" + port + "/" + database + jdbcQuery);
        if (hasText(username)) {
            properties.put("spring.datasource.username", username);
        }
        if (hasText(password)) {
            properties.put("spring.datasource.password", password);
        }
    }

    static ParsedPostgres parsePostgresUrl(String databaseUrl) {
        String normalized = databaseUrl.replaceFirst("^postgres://", "postgresql://");
        URI uri = URI.create(normalized);

        String username = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            int colon = userInfo.indexOf(':');
            username = decode(userInfo.substring(0, colon));
            password = decode(userInfo.substring(colon + 1));
        }

        String path = uri.getPath();
        String database = path != null && path.length() > 1 ? path.substring(1) : "railway";
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String host = uri.getHost();

        String query = uri.getQuery();
        String jdbcQuery;
        if (query != null && !query.isBlank()) {
            jdbcQuery = "?" + query;
        } else if (host != null && host.contains("railway.internal")) {
            jdbcQuery = "";
        } else {
            jdbcQuery = "?sslmode=require";
        }

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + jdbcQuery;
        return new ParsedPostgres(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record ParsedPostgres(String jdbcUrl, String username, String password) {}
}

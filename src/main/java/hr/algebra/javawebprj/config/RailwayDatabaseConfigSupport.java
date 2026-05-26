package hr.algebra.javawebprj.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RailwayDatabaseConfigSupport {

    private static final String SOURCE_NAME = "railwayDatabaseUrl";

    private RailwayDatabaseConfigSupport() {}

    static void applyIfNeeded(ConfigurableEnvironment environment) {
        if (hasJdbcUrl(environment)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        String postgresUrl = resolvePostgresUrl(environment);
        if (isUsablePostgresUrl(postgresUrl)) {
            applyFromUrl(properties, postgresUrl);
        } else {
            applyFromPgVars(environment, properties);
        }

        if (!properties.containsKey("spring.datasource.url")) {
            logMissingDatabaseConfig(environment);
            return;
        }

        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, properties));
    }

    private static String resolvePostgresUrl(ConfigurableEnvironment environment) {
        for (String key : List.of("DATABASE_URL", "DATABASE_PUBLIC_URL")) {
            String value = readEnv(environment, key);
            if (isUnresolvedReference(value)) {
                continue;
            }
            if (isUsablePostgresUrl(value)) {
                return value;
            }
        }
        return null;
    }

    private static void applyFromUrl(Map<String, Object> properties, String databaseUrl) {
        ParsedPostgres parsed = parsePostgresUrl(databaseUrl);
        properties.put("spring.datasource.url", parsed.jdbcUrl());
        properties.put("spring.datasource.jdbc-url", parsed.jdbcUrl());
        if (parsed.username() != null) {
            properties.put("spring.datasource.username", parsed.username());
        }
        if (parsed.password() != null) {
            properties.put("spring.datasource.password", parsed.password());
        }
    }

    private static void applyFromPgVars(ConfigurableEnvironment environment, Map<String, Object> properties) {
        String host = readEnv(environment, "PGHOST");
        if (!hasText(host) || isUnresolvedReference(host)) {
            return;
        }
        String port = readEnv(environment, "PGPORT");
        if (!hasText(port)) {
            port = "5432";
        }
        String database = readEnv(environment, "PGDATABASE");
        if (!hasText(database)) {
            database = "railway";
        }
        String username = readEnv(environment, "PGUSER");
        String password = readEnv(environment, "PGPASSWORD");

        String jdbcQuery = host.contains("railway.internal") ? "" : "?sslmode=require";
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + jdbcQuery;
        properties.put("spring.datasource.url", jdbcUrl);
        properties.put("spring.datasource.jdbc-url", jdbcUrl);
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

    private static void logMissingDatabaseConfig(ConfigurableEnvironment environment) {
        if (!environment.matchesProfiles("prod")) {
            return;
        }
        String rawDb = mask(readEnv(environment, "DATABASE_URL"));
        String rawPublic = mask(readEnv(environment, "DATABASE_PUBLIC_URL"));
        String pgHost = readEnv(environment, "PGHOST");
        System.err.println("""
                [Railway] No JDBC URL configured.
                  DATABASE_URL=%s
                  DATABASE_PUBLIC_URL=%s
                  PGHOST=%s
                  Hint: on the WEB service (not only Shared Variables), use "Add variable reference"
                  and pick Postgres → DATABASE_URL. Value must start with postgresql://
                """.formatted(rawDb, rawPublic, pgHost == null ? "(not set)" : pgHost));
    }

    private static String mask(String value) {
        if (!hasText(value)) {
            return "(not set)";
        }
        if (isUnresolvedReference(value)) {
            return "(unresolved Railway reference – link Postgres to this service)";
        }
        if (value.length() <= 24) {
            return "(set, invalid format)";
        }
        return value.substring(0, 20) + "...";
    }

    private static String readEnv(ConfigurableEnvironment environment, String key) {
        String value = environment.getProperty(key);
        if (!hasText(value)) {
            value = System.getenv(key);
        }
        return normalize(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean isUnresolvedReference(String value) {
        return hasText(value) && value.contains("${{");
    }

    private static boolean hasJdbcUrl(ConfigurableEnvironment environment) {
        return hasText(readEnv(environment, "spring.datasource.url"))
                || hasText(readEnv(environment, "spring.datasource.jdbc-url"));
    }

    private static boolean isUsablePostgresUrl(String databaseUrl) {
        if (!hasText(databaseUrl) || isUnresolvedReference(databaseUrl)) {
            return false;
        }
        String trimmed = databaseUrl.trim();
        return trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://");
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record ParsedPostgres(String jdbcUrl, String username, String password) {}
}

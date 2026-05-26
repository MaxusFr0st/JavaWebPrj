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
 * Maps Railway {@code DATABASE_URL} (postgresql://…) to {@code spring.datasource.*}
 * before auto-configuration runs. No Spring Boot 4 JDBC autoconfigure types required.
 */
public class RailwayDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "railwayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasText(environment.getProperty("spring.datasource.url"))) {
            return;
        }
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!hasText(databaseUrl)) {
            return;
        }

        ParsedPostgres parsed = parsePostgresUrl(databaseUrl.trim());
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", parsed.jdbcUrl());
        if (parsed.username() != null) {
            properties.put("spring.datasource.username", parsed.username());
        }
        if (parsed.password() != null) {
            properties.put("spring.datasource.password", parsed.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, properties));
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

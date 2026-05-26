package hr.algebra.javawebprj.config;

import org.springframework.boot.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourcePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Maps Railway's {@code DATABASE_URL} (postgresql://…) to Spring JDBC settings
 * when {@code SPRING_DATASOURCE_URL} is not set.
 */
@Configuration
public class DatabaseUrlConfig {

    @Bean
    public DataSourcePropertiesCustomizer railwayDatabaseUrlCustomizer(Environment env) {
        return properties -> {
            if (hasText(properties.getUrl())) {
                return;
            }
            String databaseUrl = env.getProperty("DATABASE_URL");
            if (!hasText(databaseUrl)) {
                return;
            }
            applyPostgresUrl(properties, databaseUrl.trim());
        };
    }

    static void applyPostgresUrl(DataSourceProperties properties, String databaseUrl) {
        String normalized = databaseUrl.replaceFirst("^postgres://", "postgresql://");
        URI uri = URI.create(normalized);

        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            int colon = userInfo.indexOf(':');
            properties.setUsername(decode(userInfo.substring(0, colon)));
            properties.setPassword(decode(userInfo.substring(colon + 1)));
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

        properties.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + jdbcQuery);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

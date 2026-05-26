package hr.algebra.javawebprj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionStartupValidator {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.seed.admin.password:}")
    private String adminSeedPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be set in production (at least 32 characters). "
                            + "Generate with: openssl rand -base64 48");
        }
        if (isWeakDefault(jwtSecret)) {
            throw new IllegalStateException("APP_JWT_SECRET must not be a sample or default value.");
        }
        if (adminSeedPassword != null && !adminSeedPassword.isBlank()) {
            if (adminSeedPassword.length() < 12) {
                throw new IllegalStateException(
                        "APP_SEED_ADMIN_PASSWORD must be at least 12 characters when set.");
            }
        }
    }

    private static boolean isWeakDefault(String secret) {
        String lower = secret.toLowerCase();
        return lower.contains("sample") || lower.contains("change_me") || lower.contains("javaweb");
    }
}

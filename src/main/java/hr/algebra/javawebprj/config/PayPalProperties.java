package hr.algebra.javawebprj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paypal")
@Getter
@Setter
public class PayPalProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String mode = "sandbox";
    private String currency = "EUR";

    public void setClientId(String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    public void setMode(String mode) {
        this.mode = mode == null ? "sandbox" : mode.trim();
    }

    public void setCurrency(String currency) {
        this.currency = currency == null ? "EUR" : currency.trim().toUpperCase();
    }

    public boolean isConfigured() {
        return hasValidClientId() && hasValidClientSecret();
    }

    public boolean hasValidClientId() {
        if (clientId.isBlank() || clientId.length() < 50) {
            return false;
        }
        String lower = clientId.toLowerCase();
        if (lower.startsWith("replace")) {
            return false;
        }
        if (lower.contains("your_client")) {
            return false;
        }
        if (lower.contains("placeholder")) {
            return false;
        }
        if (lower.contains("changeme")) {
            return false;
        }
        return true;
    }

    public boolean hasValidClientSecret() {
        if (clientSecret.isBlank() || clientSecret.length() < 50) {
            return false;
        }
        String lower = clientSecret.toLowerCase();
        if (lower.startsWith("replace")) {
            return false;
        }
        if (lower.contains("your_secret")) {
            return false;
        }
        if (lower.contains("placeholder")) {
            return false;
        }
        if (lower.contains("changeme")) {
            return false;
        }
        return true;
    }
}

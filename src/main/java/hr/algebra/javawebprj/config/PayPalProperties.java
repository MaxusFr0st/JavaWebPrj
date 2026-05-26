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
    /** sandbox or live */
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

    /** True when real PayPal sandbox/live credentials are set (not tutorial placeholders). */
    public boolean isConfigured() {
        return hasValidClientId() && hasValidClientSecret();
    }

    public boolean hasValidClientId() {
        if (clientId.isBlank() || clientId.length() < 50) {
            return false;
        }
        String lower = clientId.toLowerCase();
        return !lower.startsWith("replace")
                && !lower.contains("your_client")
                && !lower.contains("placeholder")
                && !lower.contains("changeme");
    }

    public boolean hasValidClientSecret() {
        if (clientSecret.isBlank() || clientSecret.length() < 50) {
            return false;
        }
        String lower = clientSecret.toLowerCase();
        return !lower.startsWith("replace")
                && !lower.contains("your_secret")
                && !lower.contains("placeholder")
                && !lower.contains("changeme");
    }
}

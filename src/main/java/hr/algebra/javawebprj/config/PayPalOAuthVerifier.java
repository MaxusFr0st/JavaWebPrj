package hr.algebra.javawebprj.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public final class PayPalOAuthVerifier {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private PayPalOAuthVerifier() {
    }

    public static String verify(PayPalProperties properties) {
        if (!properties.isConfigured()) {
            return "credentials not configured";
        }
        boolean live = "live".equalsIgnoreCase(properties.getMode());
        String tokenUrl;
        if (live) {
            tokenUrl = "https://api-m.paypal.com/v1/oauth2/token";
        } else {
            tokenUrl = "https://api-m.sandbox.paypal.com/v1/oauth2/token";
        }

        String creds = properties.getClientId() + ":" + properties.getClientSecret();
        String basic = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + basic)
                .header("Accept", "application/json")
                .header("Accept-Language", "en_US")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return null;
            }
            String body = response.body() == null ? "" : response.body();
            if (body.length() > 200) {
                body = body.substring(0, 200) + "...";
            }
            String envHint = live ? "live" : "sandbox";
            if (response.statusCode() == 401) {
                return "OAuth rejected (401) for " + envHint
                        + " — PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET must be from the same app;"
                        + " use Sandbox credentials when PAYPAL_MODE=sandbox. PayPal: " + body;
            }
            return "OAuth failed (HTTP " + response.statusCode() + ") for " + envHint + ": " + body;
        } catch (Exception ex) {
            return "OAuth request failed: " + ex.getMessage();
        }
    }
}

package hr.algebra.javawebprj.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.paypal.sdk.PaypalServerSdkClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayPalStartupLogger implements CommandLineRunner {

    private final PayPalProperties payPalProperties;
    private final ObjectProvider<PaypalServerSdkClient> paypalClientProvider;

    @Override
    public void run(String... args) {
        String id = payPalProperties.getClientId();
        if (!payPalProperties.isConfigured()) {
            if (!id.isBlank()) {
                log.error("PayPal: PAYPAL_CLIENT_ID looks like a placeholder ({} chars, starts with '{}') — "
                                + "set real Sandbox credentials from developer.paypal.com on Railway, then redeploy.",
                        id.length(), id.length() > 12 ? id.substring(0, 12) : id);
            } else {
                log.warn("PayPal: not configured (COD checkout still works)");
            }
            return;
        }
        String prefix = id.length() > 8 ? id.substring(0, 8) + "..." : id;
        boolean serverUp = paypalClientProvider.getIfAvailable() != null;
        log.info("PayPal: client-id {} ({}), mode={}, currency={}, server SDK={}",
                prefix, id.length(), payPalProperties.getMode(), payPalProperties.getCurrency(), serverUp ? "OK" : "MISSING");
        if (!serverUp) {
            log.warn("PayPal: server SDK bean missing — check PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET on Railway, then redeploy.");
            return;
        }
        String oauthError = PayPalOAuthVerifier.verify(payPalProperties);
        if (oauthError != null) {
            log.error("PayPal: {}", oauthError);
        } else {
            log.info("PayPal: OAuth token OK for {} mode", payPalProperties.getMode());
        }
    }
}

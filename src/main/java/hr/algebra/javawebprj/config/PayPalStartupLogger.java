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
        if (!payPalProperties.isConfigured()) {
            log.warn("PayPal: not configured (COD checkout still works)");
            return;
        }
        String id = payPalProperties.getClientId();
        String prefix = id.length() > 8 ? id.substring(0, 8) + "..." : id;
        boolean serverUp = paypalClientProvider.getIfAvailable() != null;
        log.info("PayPal: client-id {} ({}), mode={}, currency={}, server SDK={}",
                prefix, id.length(), payPalProperties.getMode(), payPalProperties.getCurrency(), serverUp ? "OK" : "MISSING");
        if (!serverUp) {
            log.warn("PayPal: server SDK bean missing — check client-id AND client-secret; only buttons may fail.");
        }
    }
}

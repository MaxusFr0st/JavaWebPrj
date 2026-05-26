package hr.algebra.javawebprj.config;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PayPalConfig {

    private final PayPalProperties payPalProperties;

    /**
     * Created only when paypal.client-id and paypal.client-secret are set in properties.
     * Do not use @ConditionalOnExpression("@payPalProperties...") — that runs before the properties bean exists.
     */
    @Bean
    @ConditionalOnProperty(prefix = "paypal", name = "client-id")
    @ConditionalOnProperty(prefix = "paypal", name = "client-secret")
    public PaypalServerSdkClient paypalServerSdkClient() {
        Environment environment = "live".equalsIgnoreCase(payPalProperties.getMode())
                ? Environment.PRODUCTION
                : Environment.SANDBOX;

        return new PaypalServerSdkClient.Builder()
                .environment(environment)
                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
                        payPalProperties.getClientId(),
                        payPalProperties.getClientSecret()
                ).build())
                .loggingConfig(builder -> builder.level(Level.INFO))
                .build();
    }
}

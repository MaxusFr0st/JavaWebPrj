package hr.algebra.javawebprj.service;

import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.AmountWithBreakdown;
import com.paypal.sdk.models.CaptureOrderInput;
import com.paypal.sdk.models.CheckoutPaymentIntent;
import com.paypal.sdk.models.CreateOrderInput;
import com.paypal.sdk.models.OrderRequest;
import com.paypal.sdk.models.PurchaseUnitRequest;
import hr.algebra.javawebprj.config.PayPalProperties;
import hr.algebra.javawebprj.dto.CartSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayPalService {

    private final ObjectProvider<PaypalServerSdkClient> paypalClientProvider;
    private final PayPalProperties payPalProperties;

    public boolean isEnabled() {
        return payPalProperties.isConfigured();
    }

    public boolean isServerReady() {
        return isEnabled() && paypalClientProvider.getIfAvailable() != null;
    }

    public String getClientId() {
        return payPalProperties.getClientId();
    }

    public String getCurrency() {
        return payPalProperties.getCurrency();
    }

    public String buildJsSdkUrl() {
        return UriComponentsBuilder
                .fromUriString("https://www.paypal.com/sdk/js")
                .queryParam("client-id", payPalProperties.getClientId())
                .queryParam("currency", payPalProperties.getCurrency())
                .queryParam("components", "buttons")
                .build()
                .toUriString();
    }

    public com.paypal.sdk.models.Order createOrder(CartSummary cart) {
        PaypalServerSdkClient client = requireClient();
        String amount = formatAmount(cart.getTotalPrice());

        CreateOrderInput input = new CreateOrderInput.Builder(
                null,
                new OrderRequest.Builder(
                        CheckoutPaymentIntent.CAPTURE,
                        List.of(new PurchaseUnitRequest.Builder(
                                new AmountWithBreakdown.Builder(
                                        payPalProperties.getCurrency(),
                                        amount
                                ).build()
                        ).build())
                ).build()
        ).build();

        try {
            ApiResponse<com.paypal.sdk.models.Order> response = client.getOrdersController().createOrder(input);
            return response.getResult();
        } catch (Exception e) {
            throw new IllegalStateException(toUserMessage("create order", e), e);
        }
    }

    public com.paypal.sdk.models.Order captureOrder(String paypalOrderId) {
        PaypalServerSdkClient client = requireClient();
        CaptureOrderInput input = new CaptureOrderInput.Builder(paypalOrderId, null).build();
        try {
            ApiResponse<com.paypal.sdk.models.Order> response = client.getOrdersController().captureOrder(input);
            return response.getResult();
        } catch (Exception e) {
            throw new IllegalStateException(toUserMessage("capture", e), e);
        }
    }

    private static String toUserMessage(String action, Exception e) {
        String detail = e.getMessage();
        if (detail == null) {
            detail = e.getClass().getSimpleName();
        }
        if (detail.contains("OAuth token") || detail.contains("not authorized") || detail.contains("ClientCredentialsAuth")) {
            return "PayPal " + action + " failed: invalid server credentials. "
                    + "The Client ID can work in the browser while the Secret is wrong. "
                    + "On Railway, set PAYPAL_CLIENT_SECRET to the Secret from the same Sandbox app as PAYPAL_CLIENT_ID "
                    + "(developer.paypal.com → your app → Sandbox). PAYPAL_MODE must be sandbox for sandbox credentials.";
        }
        return "PayPal " + action + " failed: " + detail;
    }

    private PaypalServerSdkClient requireClient() {
        PaypalServerSdkClient client = paypalClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("PayPal is not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET.");
        }
        return client;
    }

    private static String formatAmount(BigDecimal total) {
        return total.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

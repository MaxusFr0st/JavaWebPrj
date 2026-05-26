package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.api.ApiConstants;
import hr.algebra.javawebprj.dto.CartSummary;
import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.PaymentMethod;
import hr.algebra.javawebprj.service.CartService;
import hr.algebra.javawebprj.service.OrderService;
import hr.algebra.javawebprj.service.PayPalService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkout/paypal")
@RequiredArgsConstructor
public class PayPalCheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final PayPalService payPalService;

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createPayPalOrder(HttpSession session) {
        if (!payPalService.isServerReady()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody("PayPal is not configured"));
        }
        try {
            CartSummary cart = cartService.getCartSummary(session);
            if (cart.getLines().isEmpty()) {
                return ResponseEntity.badRequest().body(errorBody("Cart is empty"));
            }
            com.paypal.sdk.models.Order paypalOrder = payPalService.createOrder(cart);
            return ResponseEntity.ok(Map.of("id", paypalOrder.getId()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody(ex.getMessage() != null ? ex.getMessage() : "PayPal error"));
        }
    }

    @PostMapping("/orders/{orderId}/capture")
    public ResponseEntity<Map<String, Object>> capturePayPalOrder(
            @PathVariable("orderId") String paypalOrderId,
            HttpSession session
    ) {
        if (!payPalService.isServerReady()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody("PayPal is not configured"));
        }
        try {
            com.paypal.sdk.models.Order captured = payPalService.captureOrder(paypalOrderId);
            String status = captured.getStatus() != null ? captured.getStatus().toString() : "";

            if (!"COMPLETED".equalsIgnoreCase(status)) {
                Map<String, Object> body = new HashMap<>();
                body.put(ApiConstants.JSON_ERROR, "Payment not completed");
                body.put("status", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            Order shopOrder = orderService.placeOrder(session, PaymentMethod.PAYPAL, paypalOrderId);
            return ResponseEntity.ok(Map.of(
                    "shopOrderId", shopOrder.getId(),
                    "status", status,
                    "redirectUrl", "/checkout/confirm/" + shopOrder.getId()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody(ex.getMessage() != null ? ex.getMessage() : "PayPal error"));
        }
    }

    private static Map<String, Object> errorBody(String message) {
        return Map.of(ApiConstants.JSON_ERROR, message);
    }
}

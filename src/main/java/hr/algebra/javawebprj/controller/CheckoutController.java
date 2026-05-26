package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.dto.CartSummary;
import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.PaymentMethod;
import hr.algebra.javawebprj.service.CartService;
import hr.algebra.javawebprj.service.OrderService;
import hr.algebra.javawebprj.service.PayPalService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final PayPalService payPalService;

    @GetMapping
    public String checkoutPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        CartSummary cart = cartService.getCartSummary(session);
        if (cart.getLines().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/cart";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("paypalEnabled", payPalService.isEnabled());
        model.addAttribute("paypalServerReady", payPalService.isServerReady());
        model.addAttribute("paypalSdkUrl", payPalService.buildJsSdkUrl());
        model.addAttribute("paypalCurrency", payPalService.getCurrency());
        return "checkout/checkout";
    }

    @PostMapping("/cod")
    public String cashOnDelivery(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.placeOrder(session, PaymentMethod.COD, null);
            return "redirect:/checkout/confirm/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/confirm/{orderId}")
    public String confirm(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrderForCurrentUser(orderId);
        model.addAttribute("order", order);
        return "checkout/order-confirm";
    }
}

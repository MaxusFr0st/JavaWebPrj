package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.service.CartService;
import hr.algebra.javawebprj.service.CartSseService;
import hr.algebra.javawebprj.web.MvcConstants;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartSseService cartSseService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCartSummary(session));
        return "cart/cart";
    }

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Integer> itemCount(HttpSession session) {
        return Map.of("count", cartService.getTotalItemCount(session));
    }

    @PostMapping("/add")
    public String add(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.addProduct(session, productId, quantity);
            publishCart(session);
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Product added to cart.");
            return MvcConstants.REDIRECT_CART;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_ERROR, ex.getMessage());
            return "redirect:/shop/products/" + productId;
        }
    }

    @PostMapping("/items/{itemId}/update")
    public String updateQuantity(
            @PathVariable Long itemId,
            @RequestParam int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.updateItemQuantity(session, itemId, quantity);
            publishCart(session);
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Cart updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_ERROR, ex.getMessage());
        }
        return MvcConstants.REDIRECT_CART;
    }

    @PostMapping("/items/{itemId}/remove")
    public String removeItem(
            @PathVariable Long itemId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        cartService.removeItem(session, itemId);
        publishCart(session);
        redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Item removed.");
        return MvcConstants.REDIRECT_CART;
    }

    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes redirectAttributes) {
        cartService.clearCart(session);
        publishCart(session);
        redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Cart cleared.");
        return MvcConstants.REDIRECT_CART;
    }

    private void publishCart(HttpSession session) {
        cartSseService.publishCount(session.getId(), cartService.getTotalItemCount(session));
    }
}

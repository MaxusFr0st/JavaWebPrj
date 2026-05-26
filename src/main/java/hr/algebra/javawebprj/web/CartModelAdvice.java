package hr.algebra.javawebprj.web;

import hr.algebra.javawebprj.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class CartModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartItemCount")
    public int cartItemCount(HttpSession session) {
        try {
            return cartService.getTotalItemCount(session);
        } catch (Exception e) {
            return 0;
        }
    }
}

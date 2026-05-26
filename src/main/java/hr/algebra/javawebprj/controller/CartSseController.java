package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.service.CartService;
import hr.algebra.javawebprj.service.CartSseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartSseController {

    private final CartSseService cartSseService;
    private final CartService cartService;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpSession session) {
        int count = cartService.getTotalItemCount(session);
        return cartSseService.subscribe(session.getId(), count);
    }
}

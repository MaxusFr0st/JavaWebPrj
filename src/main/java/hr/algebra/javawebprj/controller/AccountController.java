package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String orderHistory(Model model) {
        model.addAttribute("orders", orderService.getOrdersForCurrentUser());
        return "account/orders";
    }
}

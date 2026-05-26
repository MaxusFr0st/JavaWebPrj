package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.dto.RegisterForm;
import hr.algebra.javawebprj.service.UserService;
import hr.algebra.javawebprj.web.MvcConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return MvcConstants.REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return MvcConstants.REGISTER_VIEW;
        }
        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Account created. You can log in now.");
            return "redirect:" + MvcConstants.LOGIN_PATH;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("global", ex.getMessage());
            return MvcConstants.REGISTER_VIEW;
        }
    }
}

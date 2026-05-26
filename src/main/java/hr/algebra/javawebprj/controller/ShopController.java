package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.service.CategoryService;
import hr.algebra.javawebprj.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "shop/categories";
    }

    @GetMapping("/categories/{categoryId}/products")
    public String products(@PathVariable Long categoryId, Model model) {
        model.addAttribute("category", categoryService.findById(categoryId));
        model.addAttribute("products", productService.findByCategoryId(categoryId));
        return "shop/products";
    }

    @GetMapping("/products/{productId}")
    public String productDetail(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findById(productId));
        return "shop/product-detail";
    }
}

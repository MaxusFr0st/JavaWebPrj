package hr.algebra.javawebprj.controller;

import hr.algebra.javawebprj.dto.CategoryForm;
import hr.algebra.javawebprj.dto.OrderFilterForm;
import hr.algebra.javawebprj.dto.ProductForm;
import hr.algebra.javawebprj.model.Category;
import hr.algebra.javawebprj.model.Product;
import hr.algebra.javawebprj.service.AdminOrderService;
import hr.algebra.javawebprj.service.CategoryService;
import hr.algebra.javawebprj.service.LoginAuditService;
import hr.algebra.javawebprj.service.ProductService;
import hr.algebra.javawebprj.web.MvcConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final LoginAuditService loginAuditService;
    private final AdminOrderService adminOrderService;

    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }

    // --- Categories ---

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute(MvcConstants.ATTR_CATEGORIES, categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategory(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute(MvcConstants.ATTR_EDIT_MODE, false);
        return MvcConstants.VIEW_ADMIN_CATEGORY_FORM;
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id);
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        model.addAttribute("categoryForm", form);
        model.addAttribute(MvcConstants.ATTR_EDIT_MODE, true);
        return MvcConstants.VIEW_ADMIN_CATEGORY_FORM;
    }

    @PostMapping("/categories")
    public String createCategory(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MvcConstants.ATTR_EDIT_MODE, false);
            return MvcConstants.VIEW_ADMIN_CATEGORY_FORM;
        }
        categoryService.save(form);
        return MvcConstants.REDIRECT_ADMIN_CATEGORIES;
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MvcConstants.ATTR_EDIT_MODE, true);
            return MvcConstants.VIEW_ADMIN_CATEGORY_FORM;
        }
        categoryService.update(id, form);
        return MvcConstants.REDIRECT_ADMIN_CATEGORIES;
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Category deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(MvcConstants.FLASH_ERROR, ex.getMessage());
        }
        return MvcConstants.REDIRECT_ADMIN_CATEGORIES;
    }

    // --- Products ---

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("productForm", new ProductForm());
        model.addAttribute(MvcConstants.ATTR_CATEGORIES, categoryService.findAll());
        model.addAttribute(MvcConstants.ATTR_EDIT_MODE, false);
        return MvcConstants.VIEW_ADMIN_PRODUCT_FORM;
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setStock(product.getStock());
        form.setCategoryId(product.getCategory().getId());
        model.addAttribute("productForm", form);
        model.addAttribute(MvcConstants.ATTR_CATEGORIES, categoryService.findAll());
        model.addAttribute(MvcConstants.ATTR_EDIT_MODE, true);
        return MvcConstants.VIEW_ADMIN_PRODUCT_FORM;
    }

    @PostMapping("/products")
    public String createProduct(
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MvcConstants.ATTR_CATEGORIES, categoryService.findAll());
            model.addAttribute(MvcConstants.ATTR_EDIT_MODE, false);
            return MvcConstants.VIEW_ADMIN_PRODUCT_FORM;
        }
        productService.save(form);
        return MvcConstants.REDIRECT_ADMIN_PRODUCTS;
    }

    @PostMapping("/products/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(MvcConstants.ATTR_CATEGORIES, categoryService.findAll());
            model.addAttribute(MvcConstants.ATTR_EDIT_MODE, true);
            return MvcConstants.VIEW_ADMIN_PRODUCT_FORM;
        }
        productService.update(id, form);
        return MvcConstants.REDIRECT_ADMIN_PRODUCTS;
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute(MvcConstants.FLASH_SUCCESS, "Product deleted.");
        return MvcConstants.REDIRECT_ADMIN_PRODUCTS;
    }

    // --- Login audit ---

    @GetMapping("/login-audit")
    public String loginAudit(Model model) {
        model.addAttribute("audits", loginAuditService.findAllNewestFirst());
        return "admin/login-audit";
    }

    // --- Orders ---

    @GetMapping("/orders")
    public String orders(@ModelAttribute("filter") OrderFilterForm filter, Model model) {
        model.addAttribute("orders", adminOrderService.search(filter));
        model.addAttribute("usernames", adminOrderService.allUsernames());
        return "admin/orders";
    }
}

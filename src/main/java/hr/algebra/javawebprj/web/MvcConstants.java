package hr.algebra.javawebprj.web;

public final class MvcConstants {

    private MvcConstants() {
    }

    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_VIEW = "register";

    public static final String ATTR_CATEGORIES = "categories";
    public static final String ATTR_EDIT_MODE = "editMode";

    public static final String VIEW_ADMIN_CATEGORY_FORM = "admin/category-form";
    public static final String VIEW_ADMIN_PRODUCT_FORM = "admin/product-form";

    public static final String REDIRECT_ADMIN_CATEGORIES = "redirect:/admin/categories";
    public static final String REDIRECT_ADMIN_PRODUCTS = "redirect:/admin/products";
    public static final String REDIRECT_CART = "redirect:/cart";

    public static final String FLASH_SUCCESS = "successMessage";
    public static final String FLASH_ERROR = "errorMessage";
}

package hr.algebra.javawebprj.config;

import hr.algebra.javawebprj.model.Category;
import hr.algebra.javawebprj.model.Product;
import hr.algebra.javawebprj.model.Role;
import hr.algebra.javawebprj.model.User;
import hr.algebra.javawebprj.repository.CategoryRepository;
import hr.algebra.javawebprj.repository.ProductRepository;
import hr.algebra.javawebprj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Seeds demo customer + sample catalog (categories/products).
 * Admin account is created by {@link AdminAccountSeeder}.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.customer.username:customer}")
    private String customerUsername;

    @Value("${app.seed.customer.password:}")
    private String customerPassword;

    @Value("${app.seed.customer.email:customer@shop.local}")
    private String customerEmail;

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "app.seed.catalog.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedCatalog() {
        return args -> {
            if (categoryRepository.count() == 0) {
                Category phones = categoryRepository.save(Category.builder()
                        .name("Phone accessories")
                        .description("Cases, foils, chargers")
                        .build());
                productRepository.save(Product.builder()
                        .name("Tempered glass foil")
                        .description("Universal 6.1\" screen protector")
                        .price(new BigDecimal("9.99"))
                        .stock(200)
                        .category(phones)
                        .build());
                productRepository.save(Product.builder()
                        .name("Silicone case")
                        .description("Soft cover, multiple colors")
                        .price(new BigDecimal("14.50"))
                        .stock(80)
                        .category(phones)
                        .build());
            }
        };
    }

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "app.seed.users.enabled", havingValue = "true")
    CommandLineRunner seedDemoCustomer() {
        return args -> {
            if (customerPassword == null || customerPassword.isBlank()) {
                throw new IllegalStateException(
                        "app.seed.customer.password must be set when app.seed.users.enabled=true");
            }
            if (userRepository.findByUsername(customerUsername).isEmpty()) {
                userRepository.save(User.builder()
                        .username(customerUsername)
                        .email(customerEmail)
                        .password(passwordEncoder.encode(customerPassword))
                        .role(Role.ROLE_USER)
                        .build());
            }
        };
    }
}

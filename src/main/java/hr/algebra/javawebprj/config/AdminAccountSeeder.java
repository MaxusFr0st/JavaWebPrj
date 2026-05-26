package hr.algebra.javawebprj.config;

import hr.algebra.javawebprj.model.Role;
import hr.algebra.javawebprj.model.User;
import hr.algebra.javawebprj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Code-first bootstrap of the administrator account.
 * <p>
 * Registration ({@link hr.algebra.javawebprj.service.UserService}) always creates
 * {@link Role#ROLE_USER} only — admins are never self-registered (assignment requirement).
 * This component ensures one built-in admin exists when the app starts.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.admin.password")
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    @Value("${app.seed.admin.email:admin@shop.local}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != Role.ROLE_ADMIN) {
                        existing.setRole(Role.ROLE_ADMIN);
                        userRepository.save(existing);
                        log.warn("User '{}' existed but was not ADMIN — role corrected.", adminUsername);
                    } else {
                        log.info("Admin account '{}' already present.", adminUsername);
                    }
                },
                () -> {
                    userRepository.save(User.builder()
                            .username(adminUsername)
                            .email(adminEmail)
                            .password(passwordEncoder.encode(adminPassword))
                            .role(Role.ROLE_ADMIN)
                            .build());
                    log.info("Created admin account '{}' (password from app.seed.admin.password).", adminUsername);
                }
        );
    }
}

package hr.algebra.javawebprj.config;

import hr.algebra.javawebprj.model.Role;
import hr.algebra.javawebprj.model.User;
import hr.algebra.javawebprj.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixes users created during early development (wrong role string or plain-text password).
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.users.enabled", havingValue = "true")
public class UserDataRepairRunner implements CommandLineRunner {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.customer.username:customer}")
    private String customerUsername;

    @Value("${app.seed.customer.password:}")
    private String customerPassword;

    @Override
    @Transactional
    public void run(String... args) {
        fixLegacyRoles();
        fixDemoCustomerPassword();
        warnNonBcryptPasswords();
    }

    private void fixLegacyRoles() {
        int userRoles = entityManager.createNativeQuery(
                "UPDATE users SET role = 'ROLE_USER' WHERE role = 'USER'"
        ).executeUpdate();
        int adminRoles = entityManager.createNativeQuery(
                "UPDATE users SET role = 'ROLE_ADMIN' WHERE role = 'ADMIN'"
        ).executeUpdate();
        if (userRoles + adminRoles > 0) {
            log.warn("Repaired {} user role(s) and {} admin role(s) in database.", userRoles, adminRoles);
        }
    }

    /** Ensures seeded demo customer always has a BCrypt password from configuration. */
    private void fixDemoCustomerPassword() {
        userRepository.findByUsername(customerUsername).ifPresent(user -> {
            if (!isBcrypt(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(customerPassword));
                user.setRole(Role.ROLE_USER);
                userRepository.save(user);
                log.warn("Reset demo customer '{}' password to BCrypt hash from app.seed.customer.password.", customerUsername);
            }
        });
    }

    private void warnNonBcryptPasswords() {
        for (User user : userRepository.findAll()) {
            if (!isBcrypt(user.getPassword())) {
                log.warn(
                        "User '{}' password is not BCrypt — login will fail. Delete the user and register again, or reset password.",
                        user.getUsername()
                );
            }
        }
    }

    private static boolean isBcrypt(String password) {
        return password != null && password.startsWith("$2");
    }
}

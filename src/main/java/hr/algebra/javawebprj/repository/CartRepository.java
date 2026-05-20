package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Cart;
import hr.algebra.javawebprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findBySessionId(String sessionId);
    Optional<Cart> findByUser(User user);
}
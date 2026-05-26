package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Cart;
import hr.algebra.javawebprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUser(User user);
}

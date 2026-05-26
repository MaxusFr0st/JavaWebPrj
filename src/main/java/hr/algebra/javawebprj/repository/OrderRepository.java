package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findByUserAndOrderDateBetween(User user, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findByUserUsernameContainingIgnoreCaseAndOrderDateBetweenOrderByOrderDateDesc(
            String username,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findById(Long id);
}

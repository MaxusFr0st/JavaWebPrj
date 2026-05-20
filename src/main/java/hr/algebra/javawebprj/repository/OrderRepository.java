package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    List<Order> findByUserAndOrderDateBetween(User user, LocalDateTime start, LocalDateTime end);
}
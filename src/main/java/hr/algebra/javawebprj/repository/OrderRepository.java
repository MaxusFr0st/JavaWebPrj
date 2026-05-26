package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findByUserAndOrderDateBetween(User user, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.user = :user
            ORDER BY o.orderDate DESC
            """)
    List<Order> findByUserWithItemsOrderByOrderDateDesc(@Param("user") User user);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.user u
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE (:username = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
            AND o.orderDate >= :from
            AND o.orderDate <= :to
            ORDER BY o.orderDate DESC
            """)
    List<Order> searchAdmin(
            @Param("username") String username,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByOrderByName();

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByCategoryIdOrderByName(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findById(Long id);

    boolean existsByCategoryId(Long categoryId);
}

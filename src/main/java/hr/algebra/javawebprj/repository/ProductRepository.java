package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId ORDER BY p.name")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findWithCategoryById(@Param("id") Long id);

    @Query("SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.name")
    List<Product> findAllWithCategory();

    boolean existsByCategoryId(Long categoryId);
}
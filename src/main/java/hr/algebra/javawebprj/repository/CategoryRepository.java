package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
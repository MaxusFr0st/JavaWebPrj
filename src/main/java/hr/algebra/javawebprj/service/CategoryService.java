package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.dto.CategoryForm;
import hr.algebra.javawebprj.exception.ResourceNotFoundException;
import hr.algebra.javawebprj.model.Category;
import hr.algebra.javawebprj.repository.CategoryRepository;
import hr.algebra.javawebprj.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional
    public Category save(CategoryForm form) {
        Category category = Category.builder()
                .name(form.getName().trim())
                .description(form.getDescription() != null ? form.getDescription().trim() : null)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(form.getName().trim());
        category.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category that still has products.");
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryRepository.delete(category);
    }
}

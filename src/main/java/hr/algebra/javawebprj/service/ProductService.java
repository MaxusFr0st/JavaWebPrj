package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.dto.ProductDto;
import hr.algebra.javawebprj.dto.ProductForm;
import hr.algebra.javawebprj.exception.ResourceNotFoundException;
import hr.algebra.javawebprj.model.Category;
import hr.algebra.javawebprj.model.Product;
import hr.algebra.javawebprj.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<ProductDto> findAllDto() {
        List<ProductDto> result = new ArrayList<>();
        for (Product p : productRepository.findAllByOrderByName()) {
            result.add(ProductDto.from(p));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAllByOrderByName();
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategoryId(Long categoryId) {
        categoryService.findById(categoryId);
        return productRepository.findAllByCategoryIdOrderByName(categoryId);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public ProductDto findDtoById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ProductDto.from(product);
    }

    @Transactional
    public Product save(ProductForm form) {
        Category category = categoryService.findById(form.getCategoryId());
        String desc = form.getDescription();
        if (desc != null) {
            desc = desc.trim();
        }
        Product product = Product.builder()
                .name(form.getName().trim())
                .description(desc)
                .price(form.getPrice())
                .stock(form.getStock())
                .category(category)
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductForm form) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        Category category = categoryService.findById(form.getCategoryId());
        product.setName(form.getName().trim());
        String desc = form.getDescription();
        if (desc != null) {
            desc = desc.trim();
        }
        product.setDescription(desc);
        product.setPrice(form.getPrice());
        product.setStock(form.getStock());
        product.setCategory(category);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        productRepository.delete(product);
    }
}

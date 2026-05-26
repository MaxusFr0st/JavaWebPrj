package hr.algebra.javawebprj.controller.api;

import hr.algebra.javawebprj.dto.ProductDto;
import hr.algebra.javawebprj.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> list(
            @RequestParam(required = false) Long categoryId
    ) {
        if (categoryId != null) {
            return ResponseEntity.ok(
                    productService.findByCategoryId(categoryId).stream()
                            .map(ProductDto::from)
                            .toList()
            );
        }
        return ResponseEntity.ok(productService.findAllDto());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findDtoById(id));
    }
}

package hr.algebra.javawebprj.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartLineView {
    private final Long itemId;
    private final Long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final Integer stock;
    private final BigDecimal lineTotal;
}

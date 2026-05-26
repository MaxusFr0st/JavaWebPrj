package hr.algebra.javawebprj.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartSummary {

    private final List<CartLineView> lines;
    private final int totalItems;
    private final BigDecimal totalPrice;
}

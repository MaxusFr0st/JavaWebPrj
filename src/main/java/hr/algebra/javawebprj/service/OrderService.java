package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.dto.CartLineView;
import hr.algebra.javawebprj.dto.CartSummary;
import hr.algebra.javawebprj.exception.ResourceNotFoundException;
import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.model.OrderItem;
import hr.algebra.javawebprj.model.PaymentMethod;
import hr.algebra.javawebprj.model.Product;
import hr.algebra.javawebprj.model.User;
import hr.algebra.javawebprj.repository.OrderRepository;
import hr.algebra.javawebprj.repository.ProductRepository;
import hr.algebra.javawebprj.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order placeOrder(HttpSession session, PaymentMethod paymentMethod, String paypalOrderId) {
        CartSummary cart = cartService.getCartSummary(session);
        if (cart.getLines().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        User user = currentUser();
        validateStock(cart.getLines());

        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .totalAmount(cart.getTotalPrice())
                .paypalOrderId(paypalOrderId)
                .items(new ArrayList<>())
                .build();

        for (CartLineView line : cart.getLines()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.getProductId()));

            product.setStock(product.getStock() - line.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(line.getQuantity())
                    .priceAtPurchase(line.getUnitPrice())
                    .build();
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        cartService.clearCart(session);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersForCurrentUser() {
        User user = currentUser();
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    @Transactional(readOnly = true)
    public Order getOrderForCurrentUser(Long orderId) {
        User user = currentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    private void validateStock(List<CartLineView> lines) {
        for (CartLineView line : lines) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.getProductId()));
            if (product.getStock() < line.getQuantity()) {
                throw new IllegalArgumentException(
                        "Not enough stock for " + product.getName() + ". Available: " + product.getStock());
            }
        }
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
    }
}

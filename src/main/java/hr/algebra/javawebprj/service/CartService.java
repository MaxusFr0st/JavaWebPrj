package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.dto.CartLineView;
import hr.algebra.javawebprj.dto.CartSummary;
import hr.algebra.javawebprj.model.Cart;
import hr.algebra.javawebprj.model.CartItem;
import hr.algebra.javawebprj.model.Product;
import hr.algebra.javawebprj.model.User;
import hr.algebra.javawebprj.repository.CartRepository;
import hr.algebra.javawebprj.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartSummary getCartSummary(HttpSession session) {
        Cart cart = loadCartWithItems(session);
        return toSummary(cart);
    }

    @Transactional(readOnly = true)
    public int getTotalItemCount(HttpSession session) {
        Cart cart = loadCartWithItems(session);
        int total = 0;
        for (CartItem item : cart.getItems()) {
            total += item.getQuantity();
        }
        return total;
    }

    @Transactional
    public void addProduct(HttpSession session, Long productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        Product product = productService.findById(productId);
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Not enough stock. Available: " + product.getStock());
        }

        Cart cart = getOrCreateCart(session);
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > product.getStock()) {
                throw new IllegalArgumentException("Not enough stock. Available: " + product.getStock());
            }
            item.setQuantity(newQty);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(item);
        }
        cartRepository.save(cart);
    }

    @Transactional
    public void updateItemQuantity(HttpSession session, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(session);
        CartItem item = findItemInCart(cart, itemId);
        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (quantity > item.getProduct().getStock()) {
                throw new IllegalArgumentException("Not enough stock. Available: " + item.getProduct().getStock());
            }
            item.setQuantity(quantity);
        }
        cartRepository.save(cart);
    }

    @Transactional
    public void removeItem(HttpSession session, Long itemId) {
        Cart cart = getOrCreateCart(session);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(HttpSession session) {
        Cart cart = getOrCreateCart(session);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    public void mergeSessionCartOnLogin(String sessionId, String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Optional<Cart> sessionCartOpt = cartRepository.findWithItemsBySessionId(sessionId);
        if (sessionCartOpt.isEmpty() || sessionCartOpt.get().getItems().isEmpty()) {
            return;
        }

        Cart sessionCart = sessionCartOpt.get();
        Cart userCart = cartRepository.findWithItemsByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .build()));

        for (CartItem sessionItem : sessionCart.getItems()) {
            mergeItemIntoCart(userCart, sessionItem);
        }
        cartRepository.save(userCart);
        cartRepository.delete(sessionCart);
    }

    private void mergeItemIntoCart(Cart target, CartItem sourceItem) {
        Long productId = sourceItem.getProduct().getId();
        Optional<CartItem> existing = target.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();

        int qty = sourceItem.getQuantity();
        int stock = sourceItem.getProduct().getStock();

        if (existing.isPresent()) {
            int merged = existing.get().getQuantity() + qty;
            if (merged > stock) {
                merged = stock;
            }
            existing.get().setQuantity(merged);
        } else {
            int useQty = qty;
            if (useQty > stock) {
                useQty = stock;
            }
            CartItem copy = CartItem.builder()
                    .cart(target)
                    .product(sourceItem.getProduct())
                    .quantity(useQty)
                    .build();
            target.getItems().add(copy);
        }
    }

    private Cart getOrCreateCart(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            Optional<Cart> found = cartRepository.findWithItemsByUserId(user.getId());
            if (found.isPresent()) {
                return found.get();
            }
            return cartRepository.save(Cart.builder()
                    .user(user)
                    .sessionId(session.getId())
                    .items(new ArrayList<>())
                    .build());
        }
        Optional<Cart> found = cartRepository.findWithItemsBySessionId(session.getId());
        if (found.isPresent()) {
            return found.get();
        }
        return cartRepository.save(Cart.builder()
                .sessionId(session.getId())
                .items(new ArrayList<>())
                .build());
    }

    private Cart loadCartWithItems(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            return cartRepository.findWithItemsByUserId(user.getId())
                    .orElse(Cart.builder().sessionId(session.getId()).user(user).items(List.of()).build());
        }
        return cartRepository.findWithItemsBySessionId(session.getId())
                .orElse(Cart.builder().sessionId(session.getId()).items(List.of()).build());
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        for (CartItem item : cart.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Cart item not found");
    }

    private CartSummary toSummary(Cart cart) {
        List<CartLineView> lines = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(CartLineView.builder()
                    .itemId(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .unitPrice(item.getProduct().getPrice())
                    .quantity(item.getQuantity())
                    .stock(item.getProduct().getStock())
                    .lineTotal(lineTotal)
                    .build());
        }
        lines.sort(Comparator.comparing(CartLineView::getProductName));

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (CartLineView line : lines) {
            total = total.add(line.getLineTotal());
            count += line.getQuantity();
        }

        return CartSummary.builder()
                .lines(lines)
                .totalItems(count)
                .totalPrice(total)
                .build();
    }
}

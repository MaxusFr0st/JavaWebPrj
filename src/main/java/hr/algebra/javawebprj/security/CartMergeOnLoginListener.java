package hr.algebra.javawebprj.security;

import hr.algebra.javawebprj.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * LO6 listener: merges anonymous session cart into the logged-in user's cart.
 */
@Component
@RequiredArgsConstructor
public class CartMergeOnLoginListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final CartService cartService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            return;
        }
        RequestContextHelper.currentSession().ifPresent(session ->
                cartService.mergeSessionCartOnLogin(session.getId(), userDetails.getUsername())
        );
    }
}

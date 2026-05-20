package hr.algebra.javawebprj.security;

import hr.algebra.javawebprj.model.LoginAudit;
import hr.algebra.javawebprj.repository.LoginAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuthSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final LoginAuditRepository loginAuditRepository;
    private final HttpServletRequest request; // Auto-wires current active request metadata

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();

            // Extract the user's network IP address
            String ipAddress = request.getRemoteAddr();
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = "UNKNOWN";
            }

            // Build and persist our audit entry record
            LoginAudit audit = LoginAudit.builder()
                    .username(username)
                    .ipAddress(ipAddress)
                    .loginTime(LocalDateTime.now())
                    .build();

            loginAuditRepository.save(audit);
        }
    }
}
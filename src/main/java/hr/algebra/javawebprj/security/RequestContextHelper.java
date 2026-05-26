package hr.algebra.javawebprj.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/** Reads the current HTTP request from Spring's request scope (safe for listeners). */
public final class RequestContextHelper {

    private RequestContextHelper() {
    }

    public static Optional<HttpServletRequest> currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    public static Optional<HttpSession> currentSession() {
        return currentRequest().map(request -> request.getSession(false));
    }

    public static String clientIpAddress() {
        return currentRequest()
                .map(HttpServletRequest::getRemoteAddr)
                .filter(ip -> !ip.isBlank())
                .orElse("UNKNOWN");
    }
}

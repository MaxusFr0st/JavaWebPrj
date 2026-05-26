package hr.algebra.javawebprj.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class RequestContextHelper {

    private RequestContextHelper() {
    }

    public static Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    public static Optional<HttpSession> currentSession() {
        Optional<HttpServletRequest> request = currentRequest();
        if (request.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(request.get().getSession(false));
    }

    public static String clientIpAddress() {
        Optional<HttpServletRequest> request = currentRequest();
        if (request.isEmpty()) {
            return "UNKNOWN";
        }
        String ip = request.get().getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        return ip;
    }
}

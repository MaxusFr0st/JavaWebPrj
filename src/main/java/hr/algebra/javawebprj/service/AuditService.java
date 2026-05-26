package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.model.LoginAudit;
import hr.algebra.javawebprj.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Async
    public void recordLoginAsync(String username, String ipAddress) {
        LoginAudit audit = LoginAudit.builder()
                .username(username)
                .ipAddress(ipAddress == null || ipAddress.isBlank() ? "UNKNOWN" : ipAddress)
                .loginTime(LocalDateTime.now())
                .build();
        loginAuditRepository.save(audit);
        log.debug("Async login audit saved for {}", username);
    }
}

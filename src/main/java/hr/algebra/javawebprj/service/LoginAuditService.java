package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.model.LoginAudit;
import hr.algebra.javawebprj.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Transactional(readOnly = true)
    public List<LoginAudit> findAllNewestFirst() {
        return loginAuditRepository.findAllByOrderByLoginTimeDesc();
    }
}

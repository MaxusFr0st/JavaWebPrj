package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
}
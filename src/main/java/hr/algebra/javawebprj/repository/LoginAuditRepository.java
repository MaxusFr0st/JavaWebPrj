package hr.algebra.javawebprj.repository;

import hr.algebra.javawebprj.model.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    List<LoginAudit> findAllByOrderByLoginTimeDesc();
}
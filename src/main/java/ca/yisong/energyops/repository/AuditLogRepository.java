package ca.yisong.energyops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}

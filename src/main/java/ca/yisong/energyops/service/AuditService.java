package ca.yisong.energyops.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiModels.AuditLogResponse;
import ca.yisong.energyops.model.AuditLog;
import ca.yisong.energyops.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String actor, String action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    public List<AuditLogResponse> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActor(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}

package ca.yisong.energyops.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.AuditLogResponse;
import ca.yisong.energyops.service.AuditService;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public List<AuditLogResponse> listAuditLogs() {
        return auditService.getRecentLogs();
    }
}

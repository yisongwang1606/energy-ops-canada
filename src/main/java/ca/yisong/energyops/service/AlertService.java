package ca.yisong.energyops.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.AlertResponse;
import ca.yisong.energyops.api.ApiModels.AlertUpdateRequest;
import ca.yisong.energyops.model.Alert;
import ca.yisong.energyops.model.AlertStatus;
import ca.yisong.energyops.model.PriorityLevel;
import ca.yisong.energyops.model.SensorReading;
import ca.yisong.energyops.model.SeverityLevel;
import ca.yisong.energyops.repository.AlertRepository;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AuditService auditService;
    private final long cooldownHours;

    public AlertService(
            AlertRepository alertRepository,
            AuditService auditService,
            @Value("${energy.alert.cooldown-hours}") long cooldownHours
    ) {
        this.alertRepository = alertRepository;
        this.auditService = auditService;
        this.cooldownHours = cooldownHours;
    }

    public Alert registerAlert(SensorReading reading, PredictionResult prediction, String actor, boolean logAudit) {
        if (!prediction.alertFlag() || prediction.alertType() == null || prediction.alertType().isBlank()) {
            return null;
        }

        String dedupeKey = reading.getAsset().getId() + "::" + prediction.alertType();
        Alert existing = alertRepository.findFirstByDedupeKeyAndStatusInAndLastObservedAtAfterOrderByLastObservedAtDesc(
                        dedupeKey,
                        List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED),
                        reading.getTimestamp().minusHours(cooldownHours)
                )
                .orElse(null);

        if (existing != null) {
            existing.setLastObservedAt(reading.getTimestamp());
            existing.setReading(reading);
            existing.setPriority(higherPriority(existing.getPriority(), prediction.priority()));
            existing.setSeverity(higherSeverity(existing.getSeverity(), prediction.severity()));
            if (prediction.message() != null && !prediction.message().isBlank()) {
                existing.setMessage(prediction.message());
            }
            if (prediction.recommendedAction() != null && !prediction.recommendedAction().isBlank()) {
                existing.setRecommendedAction(prediction.recommendedAction());
            }
            Alert saved = alertRepository.save(existing);
            if (logAudit) {
                auditService.log(actor, "ALERT_UPDATED", "ALERT", saved.getAlertCode(), "Deduplicated recurring alert.");
            }
            return saved;
        }

        Alert alert = new Alert();
        alert.setAlertCode("TMPA" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.CANADA));
        alert.setAlertType(prediction.alertType());
        alert.setSeverity(prediction.severity());
        alert.setStatus(AlertStatus.OPEN);
        alert.setPriority(prediction.priority());
        alert.setSite(reading.getSite());
        alert.setAsset(reading.getAsset());
        alert.setReading(reading);
        alert.setCreatedAt(reading.getTimestamp());
        alert.setLastObservedAt(reading.getTimestamp());
        alert.setMessage(prediction.message());
        alert.setRecommendedAction(prediction.recommendedAction());
        alert.setDedupeKey(dedupeKey);
        Alert saved = alertRepository.save(alert);
        saved.setAlertCode("ALT-" + String.format("%05d", saved.getId()));
        saved = alertRepository.save(saved);
        if (logAudit) {
            auditService.log(actor, "ALERT_CREATED", "ALERT", saved.getAlertCode(), saved.getAlertType());
        }
        return saved;
    }

    public List<AlertResponse> listAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AlertResponse> getActiveAlertsForAsset(String assetId) {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(alert -> alert.getAsset().getId().equals(assetId))
                .filter(alert -> alert.getStatus() != AlertStatus.RESOLVED)
                .map(this::toResponse)
                .toList();
    }

    public AlertResponse updateAlert(Long id, AlertUpdateRequest request, String actor) {
        Alert alert = getRequiredAlert(id);
        String action = request.action().trim().toUpperCase(Locale.CANADA);
        LocalDateTime now = LocalDateTime.now();
        switch (action) {
            case "ACKNOWLEDGE" -> {
                if (alert.getStatus() == AlertStatus.OPEN) {
                    alert.setStatus(AlertStatus.ACKNOWLEDGED);
                    alert.setAcknowledgedAt(now);
                }
                if (request.assignedTo() != null && !request.assignedTo().isBlank()) {
                    alert.setAssignedTo(request.assignedTo().trim());
                }
            }
            case "ASSIGN" -> {
                if (request.assignedTo() == null || request.assignedTo().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Assigned user is required.");
                }
                alert.setAssignedTo(request.assignedTo().trim());
            }
            case "RESOLVE" -> {
                alert.setStatus(AlertStatus.RESOLVED);
                if (alert.getAcknowledgedAt() == null) {
                    alert.setAcknowledgedAt(now);
                }
                alert.setResolvedAt(now);
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported alert action.");
        }
        if (request.notes() != null && !request.notes().isBlank()) {
            alert.setNotes(mergeNotes(alert.getNotes(), request.notes().trim()));
        }
        Alert saved = alertRepository.save(alert);
        auditService.log(actor, "ALERT_" + action, "ALERT", saved.getAlertCode(), saved.getAlertType());
        return toResponse(saved);
    }

    public Alert getRequiredAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found."));
    }

    public String exportAsCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("alert_code,alert_type,severity,status,priority,site_id,asset_id,created_at,assigned_to,message\n");
        alertRepository.findAllByOrderByCreatedAtDesc().forEach(alert -> builder
                .append(alert.getAlertCode()).append(',')
                .append(csv(alert.getAlertType())).append(',')
                .append(alert.getSeverity()).append(',')
                .append(alert.getStatus()).append(',')
                .append(alert.getPriority()).append(',')
                .append(alert.getSite().getId()).append(',')
                .append(alert.getAsset().getId()).append(',')
                .append(alert.getCreatedAt()).append(',')
                .append(csv(alert.getAssignedTo())).append(',')
                .append(csv(alert.getMessage())).append('\n'));
        return builder.toString();
    }

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAlertCode(),
                alert.getAlertType(),
                alert.getSeverity().name(),
                alert.getStatus().name(),
                alert.getPriority().name(),
                alert.getSite().getId(),
                alert.getSite().getName(),
                alert.getAsset().getId(),
                alert.getAsset().getName(),
                alert.getMessage(),
                alert.getRecommendedAction(),
                alert.getAssignedTo(),
                alert.getCreatedAt(),
                alert.getLastObservedAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getNotes()
        );
    }

    private String mergeNotes(String existing, String incoming) {
        return existing == null || existing.isBlank() ? incoming : existing + "\n" + incoming;
    }

    private PriorityLevel higherPriority(PriorityLevel left, PriorityLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private SeverityLevel higherSeverity(SeverityLevel left, SeverityLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

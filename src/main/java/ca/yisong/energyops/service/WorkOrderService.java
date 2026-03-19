package ca.yisong.energyops.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.WorkOrderCreateRequest;
import ca.yisong.energyops.api.ApiModels.WorkOrderResponse;
import ca.yisong.energyops.api.ApiModels.WorkOrderUpdateRequest;
import ca.yisong.energyops.model.Alert;
import ca.yisong.energyops.model.AlertStatus;
import ca.yisong.energyops.model.Asset;
import ca.yisong.energyops.model.PriorityLevel;
import ca.yisong.energyops.model.Site;
import ca.yisong.energyops.model.WorkOrder;
import ca.yisong.energyops.model.WorkOrderStatus;
import ca.yisong.energyops.repository.AlertRepository;
import ca.yisong.energyops.repository.AssetRepository;
import ca.yisong.energyops.repository.SiteRepository;
import ca.yisong.energyops.repository.WorkOrderRepository;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final AlertRepository alertRepository;
    private final AssetRepository assetRepository;
    private final SiteRepository siteRepository;
    private final AuditService auditService;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            AlertRepository alertRepository,
            AssetRepository assetRepository,
            SiteRepository siteRepository,
            AuditService auditService
    ) {
        this.workOrderRepository = workOrderRepository;
        this.alertRepository = alertRepository;
        this.assetRepository = assetRepository;
        this.siteRepository = siteRepository;
        this.auditService = auditService;
    }

    public List<WorkOrderResponse> listWorkOrders() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WorkOrderResponse> getWorkOrdersForAsset(String assetId) {
        return workOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(workOrder -> workOrder.getAsset().getId().equals(assetId))
                .map(this::toResponse)
                .toList();
    }

    public WorkOrderResponse createWorkOrder(WorkOrderCreateRequest request, String actor) {
        Alert alert = request.alertId() == null ? null : alertRepository.findById(request.alertId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found."));
        Asset asset = resolveAsset(request, alert);
        Site site = resolveSite(request, alert, asset);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode("TMPW" + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.CANADA));
        workOrder.setTitle(request.title().trim());
        workOrder.setDescription(request.description());
        workOrder.setPriority(resolvePriority(request.priority(), alert));
        workOrder.setStatus(WorkOrderStatus.OPEN);
        workOrder.setAsset(asset);
        workOrder.setSite(site);
        workOrder.setAlert(alert);
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());
        workOrder.setDueDate(request.dueDate() == null ? defaultDueDate(workOrder.getPriority()) : request.dueDate());
        workOrder.setAssignedTo(request.assignedTo());
        workOrder.setCreatedBy(actor);

        WorkOrder saved = workOrderRepository.save(workOrder);
        saved.setWorkOrderCode("WO-" + String.format("%05d", saved.getId()));
        saved = workOrderRepository.save(saved);

        if (alert != null && alert.getStatus() == AlertStatus.OPEN) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            if (saved.getAssignedTo() != null && !saved.getAssignedTo().isBlank()) {
                alert.setAssignedTo(saved.getAssignedTo());
            }
            alertRepository.save(alert);
        }

        auditService.log(actor, "WORK_ORDER_CREATED", "WORK_ORDER", saved.getWorkOrderCode(), saved.getTitle());
        return toResponse(saved);
    }

    public WorkOrderResponse updateWorkOrder(Long id, WorkOrderUpdateRequest request, String actor) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        if (request.status() != null && !request.status().isBlank()) {
            workOrder.setStatus(parseStatus(request.status()));
            if (workOrder.getStatus() == WorkOrderStatus.COMPLETED) {
                workOrder.setCompletedAt(LocalDateTime.now());
                if (workOrder.getAlert() != null && workOrder.getAlert().getStatus() != AlertStatus.RESOLVED) {
                    workOrder.getAlert().setStatus(AlertStatus.RESOLVED);
                    workOrder.getAlert().setResolvedAt(LocalDateTime.now());
                    if (workOrder.getAlert().getAcknowledgedAt() == null) {
                        workOrder.getAlert().setAcknowledgedAt(LocalDateTime.now());
                    }
                    alertRepository.save(workOrder.getAlert());
                }
            }
        }
        if (request.assignedTo() != null) {
            workOrder.setAssignedTo(request.assignedTo().isBlank() ? null : request.assignedTo().trim());
        }
        if (request.dueDate() != null) {
            workOrder.setDueDate(request.dueDate());
        }
        if (request.completionNotes() != null) {
            workOrder.setCompletionNotes(request.completionNotes());
        }
        if (request.downtimeMinutes() != null) {
            workOrder.setDowntimeMinutes(request.downtimeMinutes());
        }
        workOrder.setUpdatedAt(LocalDateTime.now());
        WorkOrder saved = workOrderRepository.save(workOrder);
        auditService.log(actor, "WORK_ORDER_UPDATED", "WORK_ORDER", saved.getWorkOrderCode(), saved.getStatus().name());
        return toResponse(saved);
    }

    public WorkOrder getRequiredWorkOrder(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Work order not found."));
    }

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
        Alert alert = workOrder.getAlert();
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getWorkOrderCode(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getPriority().name(),
                workOrder.getStatus().name(),
                workOrder.getSite().getId(),
                workOrder.getSite().getName(),
                workOrder.getAsset().getId(),
                workOrder.getAsset().getName(),
                alert == null ? null : alert.getId(),
                alert == null ? null : alert.getAlertCode(),
                workOrder.getCreatedAt(),
                workOrder.getUpdatedAt(),
                workOrder.getDueDate(),
                workOrder.getCompletedAt(),
                workOrder.getAssignedTo(),
                workOrder.getCreatedBy(),
                workOrder.getCompletionNotes(),
                workOrder.getDowntimeMinutes()
        );
    }

    private Asset resolveAsset(WorkOrderCreateRequest request, Alert alert) {
        if (alert != null) {
            return alert.getAsset();
        }
        if (request.assetId() == null || request.assetId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Asset id is required when no alert is attached.");
        }
        return assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Asset not found."));
    }

    private Site resolveSite(WorkOrderCreateRequest request, Alert alert, Asset asset) {
        if (alert != null) {
            return alert.getSite();
        }
        if (request.siteId() == null || request.siteId().isBlank()) {
            return asset.getSite();
        }
        return siteRepository.findById(request.siteId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Site not found."));
    }

    private PriorityLevel resolvePriority(String requestedPriority, Alert alert) {
        if (requestedPriority == null || requestedPriority.isBlank()) {
            return alert == null ? PriorityLevel.MEDIUM : alert.getPriority();
        }
        try {
            return PriorityLevel.valueOf(requestedPriority.trim().toUpperCase(Locale.CANADA));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported priority value.");
        }
    }

    private LocalDate defaultDueDate(PriorityLevel priority) {
        return switch (priority) {
            case HIGH, CRITICAL -> LocalDate.now().plusDays(2);
            case MEDIUM -> LocalDate.now().plusDays(4);
            case LOW -> LocalDate.now().plusDays(7);
        };
    }

    private WorkOrderStatus parseStatus(String value) {
        try {
            return WorkOrderStatus.valueOf(value.trim().toUpperCase(Locale.CANADA));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported work order status.");
        }
    }
}

package ca.yisong.energyops.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.MaintenanceRecordRequest;
import ca.yisong.energyops.api.ApiModels.MaintenanceRecordResponse;
import ca.yisong.energyops.model.MaintenanceRecord;
import ca.yisong.energyops.model.WorkOrder;
import ca.yisong.energyops.repository.MaintenanceRecordRepository;
import ca.yisong.energyops.repository.WorkOrderRepository;

@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AuditService auditService;

    public MaintenanceService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            WorkOrderRepository workOrderRepository,
            AuditService auditService
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.workOrderRepository = workOrderRepository;
        this.auditService = auditService;
    }

    public List<MaintenanceRecordResponse> listRecords() {
        return maintenanceRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MaintenanceRecordResponse createRecord(MaintenanceRecordRequest request, String actor) {
        WorkOrder workOrder = workOrderRepository.findById(request.workOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Work order not found."));

        MaintenanceRecord record = new MaintenanceRecord();
        record.setWorkOrder(workOrder);
        record.setRootCause(request.rootCause());
        record.setActionTaken(request.actionTaken());
        record.setDowntimeMinutes(request.downtimeMinutes());
        record.setPartsReplaced(request.partsReplaced());
        record.setNotes(request.notes());
        record.setCreatedAt(LocalDateTime.now());
        record.setCreatedBy(actor);

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        if (request.downtimeMinutes() != null) {
            workOrder.setDowntimeMinutes(request.downtimeMinutes());
            workOrderRepository.save(workOrder);
        }
        auditService.log(actor, "MAINTENANCE_RECORDED", "WORK_ORDER", workOrder.getWorkOrderCode(), request.rootCause());
        return toResponse(saved);
    }

    public MaintenanceRecordResponse toResponse(MaintenanceRecord record) {
        return new MaintenanceRecordResponse(
                record.getId(),
                record.getWorkOrder().getId(),
                record.getWorkOrder().getWorkOrderCode(),
                record.getRootCause(),
                record.getActionTaken(),
                record.getDowntimeMinutes(),
                record.getPartsReplaced(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getCreatedBy()
        );
    }
}

package ca.yisong.energyops.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.MaintenanceRecordRequest;
import ca.yisong.energyops.api.ApiModels.MaintenanceRecordResponse;
import ca.yisong.energyops.service.MaintenanceService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/maintenance-records")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public List<MaintenanceRecordResponse> listRecords() {
        return maintenanceService.listRecords();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER','TECHNICIAN')")
    public MaintenanceRecordResponse createRecord(@Valid @RequestBody MaintenanceRecordRequest request) {
        return maintenanceService.createRecord(request, SecurityUtils.currentUsername());
    }
}

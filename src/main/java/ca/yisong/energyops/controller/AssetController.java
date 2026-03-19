package ca.yisong.energyops.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.AssetDetailResponse;
import ca.yisong.energyops.api.ApiModels.AssetRequest;
import ca.yisong.energyops.api.ApiModels.AssetResponse;
import ca.yisong.energyops.service.AlertService;
import ca.yisong.energyops.service.AssetService;
import ca.yisong.energyops.service.SensorReadingService;
import ca.yisong.energyops.service.WorkOrderService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;
    private final WorkOrderService workOrderService;

    public AssetController(
            AssetService assetService,
            SensorReadingService sensorReadingService,
            AlertService alertService,
            WorkOrderService workOrderService
    ) {
        this.assetService = assetService;
        this.sensorReadingService = sensorReadingService;
        this.alertService = alertService;
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public List<AssetResponse> listAssets() {
        return assetService.listAssets();
    }

    @GetMapping("/{assetId}")
    public AssetDetailResponse getAsset(@PathVariable String assetId) {
        return new AssetDetailResponse(
                assetService.toResponse(assetService.getRequiredAsset(assetId)),
                sensorReadingService.getRecentReadingsForAsset(assetId, 24),
                alertService.getActiveAlertsForAsset(assetId),
                workOrderService.getWorkOrdersForAsset(assetId)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public AssetResponse createAsset(@Valid @RequestBody AssetRequest request) {
        return assetService.createOrUpdate(request, SecurityUtils.currentUsername());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public AssetResponse updateAsset(@Valid @RequestBody AssetRequest request) {
        return assetService.createOrUpdate(request, SecurityUtils.currentUsername());
    }
}

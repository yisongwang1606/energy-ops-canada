package ca.yisong.energyops.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.SensorReadingRequest;
import ca.yisong.energyops.api.ApiModels.SensorReadingResponse;
import ca.yisong.energyops.service.SensorReadingService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/sensor-readings")
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    public SensorReadingController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public SensorReadingResponse ingest(@Valid @RequestBody SensorReadingRequest request) {
        return sensorReadingService.ingest(request, SecurityUtils.currentUsername());
    }
}

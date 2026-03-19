package ca.yisong.energyops.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.AlertResponse;
import ca.yisong.energyops.api.ApiModels.AlertUpdateRequest;
import ca.yisong.energyops.service.AlertService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> listAlerts() {
        return alertService.listAlerts();
    }

    @PatchMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER','TECHNICIAN')")
    public AlertResponse updateAlert(@PathVariable Long alertId, @Valid @RequestBody AlertUpdateRequest request) {
        return alertService.updateAlert(alertId, request, SecurityUtils.currentUsername());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAlerts() {
        byte[] content = alertService.exportAsCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alerts-export.csv")
                .contentType(new MediaType("text", "csv"))
                .body(content);
    }
}

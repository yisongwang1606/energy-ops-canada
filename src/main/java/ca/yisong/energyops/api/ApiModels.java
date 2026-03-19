package ca.yisong.energyops.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ApiModels {

    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record UserView(
            String username,
            String fullName,
            String role,
            String homeProvince
    ) {
    }

    public record AuthResponse(
            String token,
            UserView user
    ) {
    }

    public record SiteRequest(
            @NotBlank @Size(max = 40) String id,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String province,
            @NotBlank @Pattern(regexp = "^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$") String postalCode,
            @NotBlank String timezone,
            @NotBlank String status,
            @Size(max = 1000) String notes
    ) {
    }

    public record SiteResponse(
            String id,
            String name,
            String city,
            String province,
            String postalCode,
            String timezone,
            String status,
            String notes
    ) {
    }

    public record AssetRequest(
            @NotBlank @Size(max = 40) String id,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 40) String assetType,
            @NotBlank String siteId,
            @NotBlank String status,
            double baselineTemperatureC,
            double temperatureSafetyMarginC,
            double vibrationThresholdMmS,
            double pressureLowKpa,
            double pressureHighKpa,
            double currentUpperAmp,
            double minimumFlowRateM3H
    ) {
    }

    public record AssetResponse(
            String id,
            String name,
            String assetType,
            String siteId,
            String siteName,
            String status,
            double baselineTemperatureC,
            double temperatureSafetyMarginC,
            double vibrationThresholdMmS,
            double pressureLowKpa,
            double pressureHighKpa,
            double currentUpperAmp,
            double minimumFlowRateM3H,
            double latestHealthScore,
            double latestFailureRisk,
            LocalDateTime latestReadingAt
    ) {
    }

    public record AssetDetailResponse(
            AssetResponse asset,
            List<SensorReadingResponse> recentReadings,
            List<AlertResponse> activeAlerts,
            List<WorkOrderResponse> workOrders
    ) {
    }

    public record SensorReadingRequest(
            String readingId,
            @NotNull LocalDateTime timestamp,
            @NotBlank String siteId,
            @NotBlank String assetId,
            @Positive double temperatureC,
            @Positive double pressureKpa,
            @Positive double vibrationMmS,
            @Positive double currentA,
            @Positive double flowRateM3H,
            Double healthScore,
            Double anomalyScore,
            Double predictedFailureRisk,
            String operatingStatus
    ) {
    }

    public record SensorReadingResponse(
            String id,
            LocalDateTime timestamp,
            String siteId,
            String siteName,
            String assetId,
            String assetName,
            String assetType,
            double temperatureC,
            double pressureKpa,
            double vibrationMmS,
            double currentA,
            double flowRateM3H,
            double healthScore,
            double anomalyScore,
            double predictedFailureRisk,
            String operatingStatus,
            boolean alertFlag,
            String alertType,
            String maintenancePriority
    ) {
    }

    public record AlertUpdateRequest(
            @NotBlank String action,
            String assignedTo,
            String notes
    ) {
    }

    public record AlertResponse(
            Long id,
            String alertCode,
            String alertType,
            String severity,
            String status,
            String priority,
            String siteId,
            String siteName,
            String assetId,
            String assetName,
            String message,
            String recommendedAction,
            String assignedTo,
            LocalDateTime createdAt,
            LocalDateTime lastObservedAt,
            LocalDateTime acknowledgedAt,
            LocalDateTime resolvedAt,
            String notes
    ) {
    }

    public record WorkOrderCreateRequest(
            Long alertId,
            String assetId,
            String siteId,
            @NotBlank String title,
            String description,
            String priority,
            String assignedTo,
            LocalDate dueDate
    ) {
    }

    public record WorkOrderUpdateRequest(
            String status,
            String assignedTo,
            LocalDate dueDate,
            String completionNotes,
            Integer downtimeMinutes
    ) {
    }

    public record WorkOrderResponse(
            Long id,
            String workOrderCode,
            String title,
            String description,
            String priority,
            String status,
            String siteId,
            String siteName,
            String assetId,
            String assetName,
            Long alertId,
            String alertCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDate dueDate,
            LocalDateTime completedAt,
            String assignedTo,
            String createdBy,
            String completionNotes,
            Integer downtimeMinutes
    ) {
    }

    public record MaintenanceRecordRequest(
            @NotNull Long workOrderId,
            @NotBlank String rootCause,
            @NotBlank String actionTaken,
            Integer downtimeMinutes,
            String partsReplaced,
            String notes
    ) {
    }

    public record MaintenanceRecordResponse(
            Long id,
            Long workOrderId,
            String workOrderCode,
            String rootCause,
            String actionTaken,
            Integer downtimeMinutes,
            String partsReplaced,
            String notes,
            LocalDateTime createdAt,
            String createdBy
    ) {
    }

    public record AuditLogResponse(
            Long id,
            String actor,
            String action,
            String entityType,
            String entityId,
            String details,
            LocalDateTime createdAt
    ) {
    }

    public record MetricCard(
            String label,
            String value,
            String detail,
            String tone
    ) {
    }

    public record TrendPoint(
            String label,
            double averageHealthScore,
            double averageFailureRisk,
            long alertCount
    ) {
    }

    public record SiteRiskSnapshot(
            String siteId,
            String siteName,
            String province,
            double averageHealthScore,
            double peakFailureRisk,
            long openAlerts
    ) {
    }

    public record AssetRiskSnapshot(
            String assetId,
            String assetName,
            String assetType,
            String siteName,
            double latestHealthScore,
            double latestFailureRisk,
            String status,
            LocalDateTime latestReadingAt
    ) {
    }

    public record DashboardResponse(
            List<MetricCard> metrics,
            List<TrendPoint> trend,
            List<SiteRiskSnapshot> siteRisk,
            List<AssetRiskSnapshot> assetRisk,
            List<AlertResponse> recentAlerts,
            List<WorkOrderResponse> openWorkOrders
    ) {
    }
}

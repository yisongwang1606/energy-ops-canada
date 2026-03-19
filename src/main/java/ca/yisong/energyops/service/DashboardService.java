package ca.yisong.energyops.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiModels.AssetRiskSnapshot;
import ca.yisong.energyops.api.ApiModels.DashboardResponse;
import ca.yisong.energyops.api.ApiModels.MetricCard;
import ca.yisong.energyops.api.ApiModels.SiteRiskSnapshot;
import ca.yisong.energyops.api.ApiModels.TrendPoint;
import ca.yisong.energyops.model.Alert;
import ca.yisong.energyops.model.AlertStatus;
import ca.yisong.energyops.model.Asset;
import ca.yisong.energyops.model.SensorReading;
import ca.yisong.energyops.model.WorkOrder;
import ca.yisong.energyops.model.WorkOrderStatus;
import ca.yisong.energyops.repository.AlertRepository;
import ca.yisong.energyops.repository.AssetRepository;
import ca.yisong.energyops.repository.SensorReadingRepository;
import ca.yisong.energyops.repository.SiteRepository;
import ca.yisong.energyops.repository.WorkOrderRepository;

@Service
public class DashboardService {

    private static final DateTimeFormatter TREND_LABEL = DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.CANADA);

    private final SiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertRepository alertRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AlertService alertService;
    private final WorkOrderService workOrderService;

    public DashboardService(
            SiteRepository siteRepository,
            AssetRepository assetRepository,
            SensorReadingRepository sensorReadingRepository,
            AlertRepository alertRepository,
            WorkOrderRepository workOrderRepository,
            AlertService alertService,
            WorkOrderService workOrderService
    ) {
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.alertRepository = alertRepository;
        this.workOrderRepository = workOrderRepository;
        this.alertService = alertService;
        this.workOrderService = workOrderService;
    }

    public DashboardResponse getOverview() {
        List<SensorReading> allReadings = sensorReadingRepository.findAllByOrderByTimestampDesc();
        List<Alert> alerts = alertRepository.findAllByOrderByCreatedAtDesc();
        List<WorkOrder> workOrders = workOrderRepository.findAllByOrderByCreatedAtDesc();
        List<Asset> assets = assetRepository.findAllByOrderByNameAsc();

        if (allReadings.isEmpty()) {
            return new DashboardResponse(
                    List.of(
                            new MetricCard("Sites", String.valueOf(siteRepository.count()), "No telemetry loaded yet", "neutral"),
                            new MetricCard("Assets", String.valueOf(assetRepository.count()), "Ready for onboarding", "neutral")
                    ),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        LocalDateTime latestTimestamp = allReadings.stream()
                .map(SensorReading::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime windowStart = latestTimestamp.minusHours(23);
        List<SensorReading> recent = allReadings.stream()
                .filter(reading -> !reading.getTimestamp().isBefore(windowStart))
                .toList();

        long openAlerts = alerts.stream().filter(alert -> alert.getStatus() != AlertStatus.RESOLVED).count();
        long activeWorkOrders = workOrders.stream()
                .filter(workOrder -> workOrder.getStatus() != WorkOrderStatus.COMPLETED && workOrder.getStatus() != WorkOrderStatus.CANCELLED)
                .count();
        double averageHealth = average(recent.stream().map(SensorReading::getHealthScore).toList());
        double averageRisk = average(recent.stream().map(SensorReading::getPredictedFailureRisk).toList());

        List<MetricCard> metrics = List.of(
                new MetricCard("Sites", String.valueOf(siteRepository.count()), "Live operating footprint", "neutral"),
                new MetricCard("Assets", String.valueOf(assetRepository.count()), assets.stream().filter(asset -> asset.getLatestFailureRisk() >= 0.25).count() + " monitored assets", "info"),
                new MetricCard("Open Alerts", String.valueOf(openAlerts), "With six-hour deduplication window", openAlerts > 0 ? "warning" : "good"),
                new MetricCard("Open Work Orders", String.valueOf(activeWorkOrders), "Tracks assignment, SLA, and closeout", activeWorkOrders > 0 ? "warning" : "good"),
                new MetricCard("Avg Health", format(averageHealth, 1), "Latest 24-hour operating window", averageHealth >= 85 ? "good" : "warning"),
                new MetricCard("Avg Failure Risk", format(averageRisk * 100.0, 1) + "%", "Based on recent telemetry", averageRisk < 0.2 ? "good" : "warning")
        );

        return new DashboardResponse(
                metrics,
                buildTrend(recent, windowStart),
                buildSiteRisk(recent, alerts),
                buildAssetRisk(assets),
                alerts.stream().limit(6).map(alertService::toResponse).toList(),
                workOrders.stream()
                        .filter(workOrder -> workOrder.getStatus() != WorkOrderStatus.COMPLETED && workOrder.getStatus() != WorkOrderStatus.CANCELLED)
                        .limit(6)
                        .map(workOrderService::toResponse)
                        .toList()
        );
    }

    private List<TrendPoint> buildTrend(List<SensorReading> recent, LocalDateTime windowStart) {
        Map<LocalDateTime, List<SensorReading>> buckets = new LinkedHashMap<>();
        LocalDateTime cursor = windowStart.withMinute(0).withSecond(0).withNano(0);
        for (int index = 0; index < 6; index++) {
            buckets.put(cursor.plusHours(index * 4L), new ArrayList<>());
        }

        for (SensorReading reading : recent) {
            long hours = java.time.Duration.between(cursor, reading.getTimestamp()).toHours();
            int bucketIndex = Math.max(0, Math.min(5, (int) (hours / 4)));
            LocalDateTime key = cursor.plusHours(bucketIndex * 4L);
            buckets.get(key).add(reading);
        }

        return buckets.entrySet().stream()
                .map(entry -> new TrendPoint(
                        entry.getKey().format(TREND_LABEL),
                        average(entry.getValue().stream().map(SensorReading::getHealthScore).toList()),
                        average(entry.getValue().stream().map(SensorReading::getPredictedFailureRisk).toList()),
                        entry.getValue().stream().filter(SensorReading::isAlertFlag).count()
                ))
                .toList();
    }

    private List<SiteRiskSnapshot> buildSiteRisk(List<SensorReading> recent, List<Alert> alerts) {
        return recent.stream()
                .collect(java.util.stream.Collectors.groupingBy(reading -> reading.getSite().getId()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<SensorReading> siteReadings = entry.getValue();
                    SensorReading sample = siteReadings.getFirst();
                    long openAlerts = alerts.stream()
                            .filter(alert -> alert.getSite().getId().equals(entry.getKey()))
                            .filter(alert -> alert.getStatus() != AlertStatus.RESOLVED)
                            .count();
                    return new SiteRiskSnapshot(
                            sample.getSite().getId(),
                            sample.getSite().getName(),
                            sample.getSite().getProvince(),
                            average(siteReadings.stream().map(SensorReading::getHealthScore).toList()),
                            siteReadings.stream().mapToDouble(SensorReading::getPredictedFailureRisk).max().orElse(0.0),
                            openAlerts
                    );
                })
                .sorted(Comparator.comparing(SiteRiskSnapshot::peakFailureRisk).reversed())
                .toList();
    }

    private List<AssetRiskSnapshot> buildAssetRisk(List<Asset> assets) {
        return assets.stream()
                .sorted(Comparator.comparing(Asset::getLatestFailureRisk).reversed())
                .limit(8)
                .map(asset -> new AssetRiskSnapshot(
                        asset.getId(),
                        asset.getName(),
                        asset.getAssetType(),
                        asset.getSite().getName(),
                        asset.getLatestHealthScore(),
                        asset.getLatestFailureRisk(),
                        asset.getStatus().name(),
                        asset.getLatestReadingAt()
                ))
                .toList();
    }

    private double average(List<Double> values) {
        return values.isEmpty()
                ? 0.0
                : Math.round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;
    }

    private String format(double value, int decimals) {
        return String.format(Locale.CANADA, "%." + decimals + "f", value);
    }
}

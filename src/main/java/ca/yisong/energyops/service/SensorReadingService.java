package ca.yisong.energyops.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.SensorReadingRequest;
import ca.yisong.energyops.api.ApiModels.SensorReadingResponse;
import ca.yisong.energyops.model.Asset;
import ca.yisong.energyops.model.AssetStatus;
import ca.yisong.energyops.model.PriorityLevel;
import ca.yisong.energyops.model.SensorReading;
import ca.yisong.energyops.model.Site;
import ca.yisong.energyops.repository.AssetRepository;
import ca.yisong.energyops.repository.SensorReadingRepository;
import ca.yisong.energyops.repository.SiteRepository;

@Service
public class SensorReadingService {

    private static final DateTimeFormatter CSV_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SensorReadingRepository sensorReadingRepository;
    private final AssetRepository assetRepository;
    private final SiteRepository siteRepository;
    private final PredictionService predictionService;
    private final AlertService alertService;
    private final AuditService auditService;

    public SensorReadingService(
            SensorReadingRepository sensorReadingRepository,
            AssetRepository assetRepository,
            SiteRepository siteRepository,
            PredictionService predictionService,
            AlertService alertService,
            AuditService auditService
    ) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.assetRepository = assetRepository;
        this.siteRepository = siteRepository;
        this.predictionService = predictionService;
        this.alertService = alertService;
        this.auditService = auditService;
    }

    @Transactional
    public SensorReadingResponse ingest(SensorReadingRequest request, String actor) {
        Asset asset = getRequiredAsset(request.assetId());
        Site site = getRequiredSite(request.siteId());
        ensureAssetSiteMatch(asset, site);
        PredictionResult prediction = predictionService.evaluate(asset, request);
        SensorReading reading = buildReading(
                request.readingId(),
                request.timestamp(),
                site,
                asset,
                request.temperatureC(),
                request.pressureKpa(),
                request.vibrationMmS(),
                request.currentA(),
                request.flowRateM3H(),
                prediction
        );

        SensorReading saved = sensorReadingRepository.save(reading);
        refreshAsset(asset, saved, prediction);
        alertService.registerAlert(saved, prediction, actor, true);
        auditService.log(actor, "READING_INGESTED", "SENSOR_READING", saved.getId(), saved.getAsset().getName());
        return toResponse(saved);
    }

    @Transactional
    public long importCsv(Resource resource, String actor) {
        if (resource == null || !resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CSV seed file not found.");
        }

        long imported = 0;
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                String readingId = csvValue(record, "reading_id");
                if (sensorReadingRepository.existsById(readingId)) {
                    continue;
                }

                Site site = getRequiredSite(csvValue(record, "site_id"));
                Asset asset = getRequiredAsset(csvValue(record, "asset_id"));
                ensureAssetSiteMatch(asset, site);

                PredictionResult prediction = predictionService.fromImportedValues(
                        asset,
                        parseDouble(csvValue(record, "temperature_c")),
                        parseDouble(csvValue(record, "pressure_kpa")),
                        parseDouble(csvValue(record, "vibration_mm_s")),
                        parseDouble(csvValue(record, "current_a")),
                        parseDouble(csvValue(record, "flow_rate_m3_h")),
                        parseNullableDouble(csvValue(record, "health_score")),
                        parseNullableDouble(csvValue(record, "anomaly_score")),
                        parseNullableDouble(csvValue(record, "predicted_failure_risk")),
                        csvValue(record, "operating_status"),
                        csvValue(record, "alert_type"),
                        "Y".equalsIgnoreCase(csvValue(record, "alert_flag"))
                );

                SensorReading reading = buildReading(
                        readingId,
                        LocalDateTime.parse(csvValue(record, "timestamp"), CSV_TIMESTAMP),
                        site,
                        asset,
                        parseDouble(csvValue(record, "temperature_c")),
                        parseDouble(csvValue(record, "pressure_kpa")),
                        parseDouble(csvValue(record, "vibration_mm_s")),
                        parseDouble(csvValue(record, "current_a")),
                        parseDouble(csvValue(record, "flow_rate_m3_h")),
                        prediction
                );
                SensorReading saved = sensorReadingRepository.save(reading);
                refreshAsset(asset, saved, prediction);
                alertService.registerAlert(saved, prediction, actor, false);
                imported++;
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to import CSV data.");
        }

        auditService.log(actor, "CSV_IMPORTED", "SENSOR_READING", String.valueOf(imported), "Imported demo sensor readings.");
        return imported;
    }

    public List<SensorReadingResponse> getRecentReadingsForAsset(String assetId, int limit) {
        return sensorReadingRepository.findByAssetIdOrderByTimestampDesc(assetId)
                .stream()
                .sorted(Comparator.comparing(SensorReading::getTimestamp).reversed())
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    public List<SensorReading> getAllReadings() {
        return sensorReadingRepository.findAllByOrderByTimestampDesc();
    }

    public SensorReadingResponse toResponse(SensorReading reading) {
        return new SensorReadingResponse(
                reading.getId(),
                reading.getTimestamp(),
                reading.getSite().getId(),
                reading.getSite().getName(),
                reading.getAsset().getId(),
                reading.getAsset().getName(),
                reading.getAsset().getAssetType(),
                reading.getTemperatureC(),
                reading.getPressureKpa(),
                reading.getVibrationMmS(),
                reading.getCurrentA(),
                reading.getFlowRateM3H(),
                reading.getHealthScore(),
                reading.getAnomalyScore(),
                reading.getPredictedFailureRisk(),
                reading.getOperatingStatus(),
                reading.isAlertFlag(),
                reading.getAlertType(),
                reading.getMaintenancePriority().name()
        );
    }

    private SensorReading buildReading(
            String requestedId,
            LocalDateTime timestamp,
            Site site,
            Asset asset,
            double temperatureC,
            double pressureKpa,
            double vibrationMmS,
            double currentA,
            double flowRateM3H,
            PredictionResult prediction
    ) {
        SensorReading reading = new SensorReading();
        reading.setId(generateReadingId(requestedId, asset, timestamp));
        reading.setTimestamp(timestamp);
        reading.setSite(site);
        reading.setAsset(asset);
        reading.setTemperatureC(temperatureC);
        reading.setPressureKpa(pressureKpa);
        reading.setVibrationMmS(vibrationMmS);
        reading.setCurrentA(currentA);
        reading.setFlowRateM3H(flowRateM3H);
        reading.setHealthScore(prediction.healthScore());
        reading.setAnomalyScore(prediction.anomalyScore());
        reading.setPredictedFailureRisk(prediction.failureRisk());
        reading.setOperatingStatus(prediction.operatingStatus());
        reading.setAlertFlag(prediction.alertFlag());
        reading.setAlertType(prediction.alertType());
        reading.setMaintenancePriority(prediction.priority());
        return reading;
    }

    private void refreshAsset(Asset asset, SensorReading reading, PredictionResult prediction) {
        if (asset.getLatestReadingAt() == null || !reading.getTimestamp().isBefore(asset.getLatestReadingAt())) {
            asset.setLatestReadingAt(reading.getTimestamp());
            asset.setLatestHealthScore(prediction.healthScore());
            asset.setLatestFailureRisk(prediction.failureRisk());
            asset.setStatus(prediction.alertFlag() ? AssetStatus.MONITOR : AssetStatus.ACTIVE);
            assetRepository.save(asset);
        }
    }

    private void ensureAssetSiteMatch(Asset asset, Site site) {
        if (!asset.getSite().getId().equals(site.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Asset and site do not match.");
        }
    }

    private Asset getRequiredAsset(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Asset not found: " + assetId));
    }

    private Site getRequiredSite(String siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Site not found: " + siteId));
    }

    private String generateReadingId(String requestedId, Asset asset, LocalDateTime timestamp) {
        if (requestedId != null && !requestedId.isBlank()) {
            return requestedId.trim();
        }
        return "ING-" + asset.getId() + "-" + timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CANADA));
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    private Double parseNullableDouble(String value) {
        return value == null || value.isBlank() ? null : Double.parseDouble(value);
    }

    private String csvValue(CSVRecord record, String header) {
        if (record.isMapped(header)) {
            return record.get(header);
        }
        for (String key : record.toMap().keySet()) {
            String normalized = key == null ? "" : key.replace("\uFEFF", "").trim();
            if (normalized.equals(header)) {
                return record.get(key);
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "CSV header not found: " + header);
    }
}

package ca.yisong.energyops.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.SensorReadingRequest;
import ca.yisong.energyops.model.Asset;
import ca.yisong.energyops.model.PriorityLevel;
import ca.yisong.energyops.model.SeverityLevel;

@Service
public class PredictionService {

    private final boolean mlEnabled;
    private final RestClient restClient;

    public PredictionService(
            @Value("${energy.ml.enabled}") boolean mlEnabled,
            @Value("${energy.ml.base-url}") String baseUrl,
            RestTemplateBuilder builder
    ) {
        this.mlEnabled = mlEnabled;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public PredictionResult evaluate(Asset asset, SensorReadingRequest request) {
        if (mlEnabled) {
            try {
                return callExternalService(asset, request);
            } catch (RestClientException ignored) {
                // Fall back to local rules if the Python sidecar is unavailable.
            }
        }
        return evaluateLocally(asset, request);
    }

    public PredictionResult fromImportedValues(
            Asset asset,
            double temperatureC,
            double pressureKpa,
            double vibrationMmS,
            double currentA,
            double flowRateM3H,
            Double healthScore,
            Double anomalyScore,
            Double predictedFailureRisk,
            String operatingStatus,
            String csvAlertType,
            boolean csvAlertFlag
    ) {
        PredictionResult fallback = evaluateLocally(
                asset,
                new SensorReadingRequest(
                        null,
                        null,
                        asset.getSite().getId(),
                        asset.getId(),
                        temperatureC,
                        pressureKpa,
                        vibrationMmS,
                        currentA,
                        flowRateM3H,
                        null,
                        null,
                        null,
                        operatingStatus
                )
        );

        double finalHealth = healthScore == null ? fallback.healthScore() : healthScore;
        double finalAnomaly = anomalyScore == null ? fallback.anomalyScore() : anomalyScore;
        double finalRisk = predictedFailureRisk == null ? fallback.failureRisk() : predictedFailureRisk;
        boolean alertFlag = csvAlertFlag || fallback.alertFlag() || finalRisk >= 0.75 || finalHealth < 70.0;
        String alertType = csvAlertType == null || csvAlertType.isBlank() ? fallback.alertType() : csvAlertType;
        PriorityLevel priority = finalRisk >= 0.75 || finalHealth < 70.0
                ? PriorityLevel.HIGH
                : (alertFlag ? PriorityLevel.MEDIUM : PriorityLevel.LOW);
        SeverityLevel severity = priority == PriorityLevel.HIGH ? SeverityLevel.HIGH : SeverityLevel.WARNING;
        String status = operatingStatus == null || operatingStatus.isBlank()
                ? fallback.operatingStatus()
                : operatingStatus;

        return new PredictionResult(
                finalHealth,
                finalAnomaly,
                finalRisk,
                alertFlag,
                alertType,
                priority,
                severity,
                status,
                fallback.recommendedAction(),
                fallback.message()
        );
    }

    private PredictionResult callExternalService(Asset asset, SensorReadingRequest request) {
        MlScoreResponse healthResponse = restClient.post()
                .uri("/ml/predict/health-score")
                .body(new MlScoreRequest(asset, request))
                .retrieve()
                .body(MlScoreResponse.class);
        MlRiskResponse riskResponse = restClient.post()
                .uri("/ml/predict/failure-risk")
                .body(new MlScoreRequest(asset, request))
                .retrieve()
                .body(MlRiskResponse.class);

        if (healthResponse == null || riskResponse == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ML service returned an empty response.");
        }

        return new PredictionResult(
                healthResponse.healthScore(),
                healthResponse.anomalyScore(),
                riskResponse.failureRisk(),
                riskResponse.alertFlag(),
                riskResponse.alertType(),
                PriorityLevel.valueOf(riskResponse.priority()),
                SeverityLevel.valueOf(riskResponse.severity()),
                riskResponse.operatingStatus(),
                riskResponse.recommendedAction(),
                riskResponse.message()
        );
    }

    private PredictionResult evaluateLocally(Asset asset, SensorReadingRequest request) {
        double tempLimit = asset.getBaselineTemperatureC() + asset.getTemperatureSafetyMarginC();
        double tempOver = Math.max(0.0, request.temperatureC() - tempLimit);
        double tempAboveBaseline = Math.max(0.0, request.temperatureC() - asset.getBaselineTemperatureC());
        double vibrationOver = Math.max(0.0, request.vibrationMmS() - asset.getVibrationThresholdMmS());
        double lowPressureGap = Math.max(0.0, asset.getPressureLowKpa() - request.pressureKpa());
        double highPressureGap = Math.max(0.0, request.pressureKpa() - asset.getPressureHighKpa());
        double pressureDeviation = Math.max(lowPressureGap, highPressureGap);
        double currentOver = Math.max(0.0, request.currentA() - asset.getCurrentUpperAmp());
        double flowDeficit = Math.max(0.0, asset.getMinimumFlowRateM3H() - request.flowRateM3H());

        double risk = clamp(
                0.08
                        + tempOver * 0.02
                        + vibrationOver * 0.08
                        + pressureDeviation * 0.0006
                        + currentOver * 0.008
                        + flowDeficit * 0.003,
                0.05,
                0.99
        );

        double health = clamp(
                98.0
                        - tempAboveBaseline * 0.75
                        - vibrationOver * 14.0
                        - pressureDeviation * 0.03
                        - currentOver * 0.18
                        - flowDeficit * 0.09,
                12.0,
                99.0
        );

        double anomaly = clamp(
                0.04 + risk * 1.05 + tempOver * 0.01 + vibrationOver * 0.04 + pressureDeviation * 0.0005,
                0.02,
                0.99
        );

        String alertType = null;
        String recommendedAction = "Continue monitoring.";
        String message = "Asset is operating within the expected range.";

        double tempScore = tempOver / Math.max(1.0, asset.getTemperatureSafetyMarginC());
        double vibrationScore = vibrationOver / Math.max(1.0, asset.getVibrationThresholdMmS());
        double pressureScore = pressureDeviation / Math.max(1.0, asset.getPressureLowKpa() * 0.1);
        double currentScore = currentOver / Math.max(1.0, asset.getCurrentUpperAmp() * 0.1);

        double maxScore = Math.max(Math.max(tempScore, vibrationScore), Math.max(pressureScore, currentScore));
        if (maxScore > 0.0) {
            if (maxScore == tempScore) {
                alertType = "High Temperature";
                recommendedAction = "Inspect cooling efficiency and lubricants before the next shift handover.";
                message = "Temperature exceeded the Canadian operating safety margin.";
            } else if (maxScore == vibrationScore) {
                alertType = "High Vibration";
                recommendedAction = "Schedule a vibration check and confirm alignment or bearing wear.";
                message = "Vibration crossed the equipment threshold for predictive maintenance.";
            } else if (maxScore == pressureScore) {
                alertType = "Pressure Deviation";
                recommendedAction = "Validate line pressure, valve position, and winterization impacts.";
                message = "Pressure drift moved outside the stable operating envelope.";
            } else {
                alertType = "Current Deviation";
                recommendedAction = "Check electrical load and motor condition.";
                message = "Current draw is above the expected level for this asset.";
            }
        }

        boolean alertFlag = alertType != null || risk >= 0.75 || health < 70.0;
        PriorityLevel priority = risk >= 0.82 || health < 60.0
                ? PriorityLevel.HIGH
                : (alertFlag ? PriorityLevel.MEDIUM : PriorityLevel.LOW);
        SeverityLevel severity = priority == PriorityLevel.HIGH ? SeverityLevel.HIGH : SeverityLevel.WARNING;
        String operatingStatus = priority == PriorityLevel.HIGH ? "Monitor" : "Normal";

        return new PredictionResult(
                round(health),
                round(anomaly),
                round(risk),
                alertFlag,
                alertType,
                priority,
                severity,
                operatingStatus,
                recommendedAction,
                message
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record MlScoreRequest(
            String assetId,
            String assetType,
            double baselineTemperatureC,
            double temperatureSafetyMarginC,
            double vibrationThresholdMmS,
            double pressureLowKpa,
            double pressureHighKpa,
            double currentUpperAmp,
            double minimumFlowRateM3H,
            double temperatureC,
            double pressureKpa,
            double vibrationMmS,
            double currentA,
            double flowRateM3H
    ) {
        private MlScoreRequest(Asset asset, SensorReadingRequest request) {
            this(
                    asset.getId(),
                    asset.getAssetType(),
                    asset.getBaselineTemperatureC(),
                    asset.getTemperatureSafetyMarginC(),
                    asset.getVibrationThresholdMmS(),
                    asset.getPressureLowKpa(),
                    asset.getPressureHighKpa(),
                    asset.getCurrentUpperAmp(),
                    asset.getMinimumFlowRateM3H(),
                    request.temperatureC(),
                    request.pressureKpa(),
                    request.vibrationMmS(),
                    request.currentA(),
                    request.flowRateM3H()
            );
        }
    }

    private record MlScoreResponse(double healthScore, double anomalyScore) {
    }

    private record MlRiskResponse(
            double failureRisk,
            boolean alertFlag,
            String alertType,
            String priority,
            String severity,
            String operatingStatus,
            String recommendedAction,
            String message
    ) {
    }
}

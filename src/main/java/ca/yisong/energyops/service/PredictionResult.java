package ca.yisong.energyops.service;

import ca.yisong.energyops.model.PriorityLevel;
import ca.yisong.energyops.model.SeverityLevel;

public record PredictionResult(
        double healthScore,
        double anomalyScore,
        double failureRisk,
        boolean alertFlag,
        String alertType,
        PriorityLevel priority,
        SeverityLevel severity,
        String operatingStatus,
        String recommendedAction,
        String message
) {
}

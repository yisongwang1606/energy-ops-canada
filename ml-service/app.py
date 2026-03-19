from fastapi import FastAPI
from pydantic import BaseModel


app = FastAPI(title="Energy Ops ML Service", version="0.1.0")


class PredictionRequest(BaseModel):
    assetId: str
    assetType: str
    baselineTemperatureC: float
    temperatureSafetyMarginC: float
    vibrationThresholdMmS: float
    pressureLowKpa: float
    pressureHighKpa: float
    currentUpperAmp: float
    minimumFlowRateM3H: float
    temperatureC: float
    pressureKpa: float
    vibrationMmS: float
    currentA: float
    flowRateM3H: float


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def evaluate(payload: PredictionRequest) -> dict:
    temp_limit = payload.baselineTemperatureC + payload.temperatureSafetyMarginC
    temp_over = max(0.0, payload.temperatureC - temp_limit)
    temp_above_baseline = max(0.0, payload.temperatureC - payload.baselineTemperatureC)
    vibration_over = max(0.0, payload.vibrationMmS - payload.vibrationThresholdMmS)
    low_pressure_gap = max(0.0, payload.pressureLowKpa - payload.pressureKpa)
    high_pressure_gap = max(0.0, payload.pressureKpa - payload.pressureHighKpa)
    pressure_deviation = max(low_pressure_gap, high_pressure_gap)
    current_over = max(0.0, payload.currentA - payload.currentUpperAmp)
    flow_deficit = max(0.0, payload.minimumFlowRateM3H - payload.flowRateM3H)

    failure_risk = clamp(
        0.08
        + temp_over * 0.02
        + vibration_over * 0.08
        + pressure_deviation * 0.0006
        + current_over * 0.008
        + flow_deficit * 0.003,
        0.05,
        0.99,
    )

    health_score = clamp(
        98.0
        - temp_above_baseline * 0.75
        - vibration_over * 14.0
        - pressure_deviation * 0.03
        - current_over * 0.18
        - flow_deficit * 0.09,
        12.0,
        99.0,
    )

    anomaly_score = clamp(
        0.04 + failure_risk * 1.05 + temp_over * 0.01 + vibration_over * 0.04 + pressure_deviation * 0.0005,
        0.02,
        0.99,
    )

    temp_score = temp_over / max(1.0, payload.temperatureSafetyMarginC)
    vibration_score = vibration_over / max(1.0, payload.vibrationThresholdMmS)
    pressure_score = pressure_deviation / max(1.0, payload.pressureLowKpa * 0.1)
    current_score = current_over / max(1.0, payload.currentUpperAmp * 0.1)
    max_score = max(temp_score, vibration_score, pressure_score, current_score)

    alert_type = None
    recommended_action = "Continue monitoring."
    message = "Asset is operating within the expected range."
    if max_score > 0.0:
        if max_score == temp_score:
            alert_type = "High Temperature"
            recommended_action = "Inspect cooling efficiency and lubricants before the next shift handover."
            message = "Temperature exceeded the Canadian operating safety margin."
        elif max_score == vibration_score:
            alert_type = "High Vibration"
            recommended_action = "Schedule a vibration check and confirm alignment or bearing wear."
            message = "Vibration crossed the equipment threshold for predictive maintenance."
        elif max_score == pressure_score:
            alert_type = "Pressure Deviation"
            recommended_action = "Validate line pressure, valve position, and winterization impacts."
            message = "Pressure drift moved outside the stable operating envelope."
        else:
            alert_type = "Current Deviation"
            recommended_action = "Check electrical load and motor condition."
            message = "Current draw is above the expected level for this asset."

    alert_flag = alert_type is not None or failure_risk >= 0.75 or health_score < 70.0
    priority = "HIGH" if failure_risk >= 0.82 or health_score < 60.0 else ("MEDIUM" if alert_flag else "LOW")
    severity = "HIGH" if priority == "HIGH" else "WARNING"
    operating_status = "Monitor" if priority == "HIGH" else "Normal"

    return {
        "healthScore": round(health_score, 3),
        "anomalyScore": round(anomaly_score, 3),
        "failureRisk": round(failure_risk, 3),
        "alertFlag": alert_flag,
        "alertType": alert_type,
        "priority": priority,
        "severity": severity,
        "operatingStatus": operating_status,
        "recommendedAction": recommended_action,
        "message": message,
    }


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "service": "energy-ops-ml"}


@app.post("/ml/predict/health-score")
def health_score(payload: PredictionRequest) -> dict:
    result = evaluate(payload)
    return {
        "healthScore": result["healthScore"],
        "anomalyScore": result["anomalyScore"],
    }


@app.post("/ml/predict/failure-risk")
def failure_risk(payload: PredictionRequest) -> dict:
    result = evaluate(payload)
    return {
        "failureRisk": result["failureRisk"],
        "alertFlag": result["alertFlag"],
        "alertType": result["alertType"],
        "priority": result["priority"],
        "severity": result["severity"],
        "operatingStatus": result["operatingStatus"],
        "recommendedAction": result["recommendedAction"],
        "message": result["message"],
    }

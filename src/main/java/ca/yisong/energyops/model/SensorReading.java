package ca.yisong.energyops.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @Column(name = "reading_id", nullable = false, length = 40)
    private String id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private double temperatureC;

    @Column(nullable = false)
    private double pressureKpa;

    @Column(nullable = false)
    private double vibrationMmS;

    @Column(nullable = false)
    private double currentA;

    @Column(nullable = false)
    private double flowRateM3H;

    @Column(nullable = false)
    private double healthScore;

    @Column(nullable = false)
    private double anomalyScore;

    @Column(nullable = false)
    private double predictedFailureRisk;

    @Column(nullable = false)
    private String operatingStatus;

    @Column(nullable = false)
    private boolean alertFlag;

    @Column(length = 80)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriorityLevel maintenancePriority = PriorityLevel.LOW;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public double getPressureKpa() {
        return pressureKpa;
    }

    public void setPressureKpa(double pressureKpa) {
        this.pressureKpa = pressureKpa;
    }

    public double getVibrationMmS() {
        return vibrationMmS;
    }

    public void setVibrationMmS(double vibrationMmS) {
        this.vibrationMmS = vibrationMmS;
    }

    public double getCurrentA() {
        return currentA;
    }

    public void setCurrentA(double currentA) {
        this.currentA = currentA;
    }

    public double getFlowRateM3H() {
        return flowRateM3H;
    }

    public void setFlowRateM3H(double flowRateM3H) {
        this.flowRateM3H = flowRateM3H;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public double getPredictedFailureRisk() {
        return predictedFailureRisk;
    }

    public void setPredictedFailureRisk(double predictedFailureRisk) {
        this.predictedFailureRisk = predictedFailureRisk;
    }

    public String getOperatingStatus() {
        return operatingStatus;
    }

    public void setOperatingStatus(String operatingStatus) {
        this.operatingStatus = operatingStatus;
    }

    public boolean isAlertFlag() {
        return alertFlag;
    }

    public void setAlertFlag(boolean alertFlag) {
        this.alertFlag = alertFlag;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public PriorityLevel getMaintenancePriority() {
        return maintenancePriority;
    }

    public void setMaintenancePriority(PriorityLevel maintenancePriority) {
        this.maintenancePriority = maintenancePriority;
    }
}

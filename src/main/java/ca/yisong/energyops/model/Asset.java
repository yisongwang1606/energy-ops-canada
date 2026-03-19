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
@Table(name = "assets")
public class Asset {

    @Id
    @Column(name = "asset_id", nullable = false, length = 40)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 40)
    private String assetType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(nullable = false)
    private double baselineTemperatureC;

    @Column(nullable = false)
    private double temperatureSafetyMarginC;

    @Column(nullable = false)
    private double vibrationThresholdMmS;

    @Column(nullable = false)
    private double pressureLowKpa;

    @Column(nullable = false)
    private double pressureHighKpa;

    @Column(nullable = false)
    private double currentUpperAmp;

    @Column(nullable = false)
    private double minimumFlowRateM3H;

    @Column(nullable = false)
    private double latestHealthScore = 100.0;

    @Column(nullable = false)
    private double latestFailureRisk = 0.0;

    private LocalDateTime latestReadingAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public double getBaselineTemperatureC() {
        return baselineTemperatureC;
    }

    public void setBaselineTemperatureC(double baselineTemperatureC) {
        this.baselineTemperatureC = baselineTemperatureC;
    }

    public double getTemperatureSafetyMarginC() {
        return temperatureSafetyMarginC;
    }

    public void setTemperatureSafetyMarginC(double temperatureSafetyMarginC) {
        this.temperatureSafetyMarginC = temperatureSafetyMarginC;
    }

    public double getVibrationThresholdMmS() {
        return vibrationThresholdMmS;
    }

    public void setVibrationThresholdMmS(double vibrationThresholdMmS) {
        this.vibrationThresholdMmS = vibrationThresholdMmS;
    }

    public double getPressureLowKpa() {
        return pressureLowKpa;
    }

    public void setPressureLowKpa(double pressureLowKpa) {
        this.pressureLowKpa = pressureLowKpa;
    }

    public double getPressureHighKpa() {
        return pressureHighKpa;
    }

    public void setPressureHighKpa(double pressureHighKpa) {
        this.pressureHighKpa = pressureHighKpa;
    }

    public double getCurrentUpperAmp() {
        return currentUpperAmp;
    }

    public void setCurrentUpperAmp(double currentUpperAmp) {
        this.currentUpperAmp = currentUpperAmp;
    }

    public double getMinimumFlowRateM3H() {
        return minimumFlowRateM3H;
    }

    public void setMinimumFlowRateM3H(double minimumFlowRateM3H) {
        this.minimumFlowRateM3H = minimumFlowRateM3H;
    }

    public double getLatestHealthScore() {
        return latestHealthScore;
    }

    public void setLatestHealthScore(double latestHealthScore) {
        this.latestHealthScore = latestHealthScore;
    }

    public double getLatestFailureRisk() {
        return latestFailureRisk;
    }

    public void setLatestFailureRisk(double latestFailureRisk) {
        this.latestFailureRisk = latestFailureRisk;
    }

    public LocalDateTime getLatestReadingAt() {
        return latestReadingAt;
    }

    public void setLatestReadingAt(LocalDateTime latestReadingAt) {
        this.latestReadingAt = latestReadingAt;
    }
}

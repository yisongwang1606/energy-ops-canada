package ca.yisong.energyops.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.AssetRequest;
import ca.yisong.energyops.api.ApiModels.AssetResponse;
import ca.yisong.energyops.model.Asset;
import ca.yisong.energyops.model.AssetStatus;
import ca.yisong.energyops.repository.AssetRepository;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final SiteService siteService;
    private final AuditService auditService;

    public AssetService(
            AssetRepository assetRepository,
            SiteService siteService,
            AuditService auditService
    ) {
        this.assetRepository = assetRepository;
        this.siteService = siteService;
        this.auditService = auditService;
    }

    public List<AssetResponse> listAssets() {
        return assetRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssetResponse createOrUpdate(AssetRequest request, String actor) {
        Asset asset = assetRepository.findById(request.id()).orElseGet(Asset::new);
        boolean created = asset.getId() == null;
        asset.setId(request.id().trim().toUpperCase(Locale.CANADA));
        asset.setName(request.name().trim());
        asset.setAssetType(request.assetType().trim());
        asset.setSite(siteService.getRequiredSite(request.siteId()));
        asset.setStatus(parseStatus(request.status()));
        asset.setBaselineTemperatureC(request.baselineTemperatureC());
        asset.setTemperatureSafetyMarginC(request.temperatureSafetyMarginC());
        asset.setVibrationThresholdMmS(request.vibrationThresholdMmS());
        asset.setPressureLowKpa(request.pressureLowKpa());
        asset.setPressureHighKpa(request.pressureHighKpa());
        asset.setCurrentUpperAmp(request.currentUpperAmp());
        asset.setMinimumFlowRateM3H(request.minimumFlowRateM3H());
        Asset saved = assetRepository.save(asset);
        auditService.log(actor, created ? "ASSET_CREATED" : "ASSET_UPDATED", "ASSET", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public Asset getRequiredAsset(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Asset not found: " + assetId));
    }

    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getAssetType(),
                asset.getSite().getId(),
                asset.getSite().getName(),
                asset.getStatus().name(),
                asset.getBaselineTemperatureC(),
                asset.getTemperatureSafetyMarginC(),
                asset.getVibrationThresholdMmS(),
                asset.getPressureLowKpa(),
                asset.getPressureHighKpa(),
                asset.getCurrentUpperAmp(),
                asset.getMinimumFlowRateM3H(),
                asset.getLatestHealthScore(),
                asset.getLatestFailureRisk(),
                asset.getLatestReadingAt()
        );
    }

    private AssetStatus parseStatus(String value) {
        try {
            return AssetStatus.valueOf(value.trim().toUpperCase(Locale.CANADA));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported asset status.");
        }
    }
}

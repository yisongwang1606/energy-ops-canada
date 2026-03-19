package ca.yisong.energyops.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.yisong.energyops.api.ApiException;
import ca.yisong.energyops.api.ApiModels.SiteRequest;
import ca.yisong.energyops.api.ApiModels.SiteResponse;
import ca.yisong.energyops.model.Site;
import ca.yisong.energyops.model.SiteStatus;
import ca.yisong.energyops.repository.SiteRepository;
import ca.yisong.energyops.support.CanadianLocaleRules;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final CanadianLocaleRules canadianLocaleRules;
    private final AuditService auditService;

    public SiteService(SiteRepository siteRepository, CanadianLocaleRules canadianLocaleRules, AuditService auditService) {
        this.siteRepository = siteRepository;
        this.canadianLocaleRules = canadianLocaleRules;
        this.auditService = auditService;
    }

    public List<SiteResponse> listSites() {
        return siteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SiteResponse createOrUpdate(SiteRequest request, String actor) {
        Site site = siteRepository.findById(request.id()).orElseGet(Site::new);
        boolean created = site.getId() == null;
        site.setId(request.id().trim().toUpperCase(Locale.CANADA));
        site.setName(request.name().trim());
        site.setCity(request.city().trim());
        site.setProvince(canadianLocaleRules.normalizeProvince(request.province()));
        site.setPostalCode(canadianLocaleRules.normalizePostalCode(request.postalCode()));
        site.setTimezone(request.timezone().trim());
        site.setStatus(parseStatus(request.status()));
        site.setNotes(request.notes());
        Site saved = siteRepository.save(site);
        auditService.log(actor, created ? "SITE_CREATED" : "SITE_UPDATED", "SITE", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public Site getRequiredSite(String siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Site not found: " + siteId));
    }

    public SiteResponse toResponse(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getName(),
                site.getCity(),
                site.getProvince(),
                site.getPostalCode(),
                site.getTimezone(),
                site.getStatus().name(),
                site.getNotes()
        );
    }

    private SiteStatus parseStatus(String value) {
        try {
            return SiteStatus.valueOf(value.trim().toUpperCase(Locale.CANADA));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported site status.");
        }
    }
}

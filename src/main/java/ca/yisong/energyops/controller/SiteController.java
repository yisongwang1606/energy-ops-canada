package ca.yisong.energyops.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.SiteRequest;
import ca.yisong.energyops.api.ApiModels.SiteResponse;
import ca.yisong.energyops.service.SiteService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public List<SiteResponse> listSites() {
        return siteService.listSites();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public SiteResponse createSite(@Valid @RequestBody SiteRequest request) {
        return siteService.createOrUpdate(request, SecurityUtils.currentUsername());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public SiteResponse updateSite(@Valid @RequestBody SiteRequest request) {
        return siteService.createOrUpdate(request, SecurityUtils.currentUsername());
    }
}

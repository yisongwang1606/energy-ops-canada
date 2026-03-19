package ca.yisong.energyops.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.Site;

public interface SiteRepository extends JpaRepository<Site, String> {
}

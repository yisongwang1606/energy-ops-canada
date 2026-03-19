package ca.yisong.energyops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.Asset;

public interface AssetRepository extends JpaRepository<Asset, String> {

    List<Asset> findAllByOrderByNameAsc();

    List<Asset> findBySiteIdOrderByNameAsc(String siteId);
}

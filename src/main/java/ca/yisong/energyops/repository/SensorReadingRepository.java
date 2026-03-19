package ca.yisong.energyops.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.SensorReading;

public interface SensorReadingRepository extends JpaRepository<SensorReading, String> {

    List<SensorReading> findAllByOrderByTimestampDesc();

    List<SensorReading> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime start, LocalDateTime end);

    List<SensorReading> findByAssetIdOrderByTimestampDesc(String assetId);

    List<SensorReading> findTop24ByAssetIdOrderByTimestampDesc(String assetId);
}

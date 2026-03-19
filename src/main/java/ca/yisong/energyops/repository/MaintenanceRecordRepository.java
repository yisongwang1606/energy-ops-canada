package ca.yisong.energyops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.MaintenanceRecord;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findAllByOrderByCreatedAtDesc();

    List<MaintenanceRecord> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
}

package ca.yisong.energyops.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findAllByOrderByCreatedAtDesc();
}

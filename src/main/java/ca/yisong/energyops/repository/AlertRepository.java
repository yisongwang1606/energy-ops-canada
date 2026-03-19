package ca.yisong.energyops.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.Alert;
import ca.yisong.energyops.model.AlertStatus;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    Optional<Alert> findFirstByDedupeKeyAndStatusInAndLastObservedAtAfterOrderByLastObservedAtDesc(
            String dedupeKey,
            Collection<AlertStatus> statuses,
            LocalDateTime after
    );
}

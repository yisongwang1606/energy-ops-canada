package ca.yisong.energyops.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.yisong.energyops.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);
}

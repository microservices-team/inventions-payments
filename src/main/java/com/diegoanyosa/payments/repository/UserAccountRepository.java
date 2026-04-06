package com.diegoanyosa.payments.repository;

import com.diegoanyosa.payments.command.model.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT a FROM UserAccount a WHERE a.userId = :userId AND a.active = true")
    Optional<UserAccount> findActiveByUserIdForUpdate(String userId);

    Optional<UserAccount> findByUserIdAndActiveTrue(String userId);
}

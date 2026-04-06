package com.diegoanyosa.payments.repository;

import com.diegoanyosa.payments.command.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByCommandId(String commandId);
    boolean existsByCommandId(String commandId);
}

package com.diegoanyosa.payments.repository;

import com.diegoanyosa.payments.query.model.TransactionView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TransactionViewRepository extends JpaRepository<TransactionView, UUID> {

    /** Historia completa (enviadas + recibidas), opcionalmente filtrada por status */
    @Query("""
        SELECT t FROM TransactionView t
        WHERE (t.senderId = :userId OR t.recipientId = :userId)
          AND (:status IS NULL OR t.status = :status)
        ORDER BY t.createdAt DESC
        """)
    Page<TransactionView> findHistory(
        @Param("userId") String userId,
        @Param("status") String status,
        Pageable pageable);

    /**
     * GET /api/payments/transactions?direction=SENT|RECEIVED&status=COMPLETED|FAILED|...
     * Una sola API unificada para enviadas y recibidas, filtrada por status.
     */
    @Query("""
        SELECT t FROM TransactionView t
        WHERE (
            (:direction = 'SENT'     AND t.senderId    = :userId) OR
            (:direction = 'RECEIVED' AND t.recipientId = :userId) OR
            (:direction = 'ALL'      AND (t.senderId = :userId OR t.recipientId = :userId))
        )
        AND (:status IS NULL OR t.status = :status)
        ORDER BY t.createdAt DESC
        """)
    Page<TransactionView> findByDirectionAndStatus(
        @Param("userId")    String userId,
        @Param("direction") String direction,
        @Param("status")    String status,
        Pageable pageable);
}

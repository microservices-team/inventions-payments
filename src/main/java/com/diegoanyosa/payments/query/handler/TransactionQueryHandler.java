package com.diegoanyosa.payments.query.handler;

import com.diegoanyosa.payments.command.model.UserAccount;
import com.diegoanyosa.payments.dto.response.*;
import com.diegoanyosa.payments.event.TransactionEvent;
import com.diegoanyosa.payments.exception.AccountNotFoundException;
import com.diegoanyosa.payments.query.model.TransactionView;
import com.diegoanyosa.payments.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionQueryHandler {

    private final TransactionViewRepository viewRepository;
    private final UserAccountRepository     accountRepository;

    // ── Projection updater ─────────────────────────────────────────────

    @EventListener
    @Transactional
    public void on(TransactionEvent event) {
        log.debug("Projecting event: {} status={}", event.getTransactionId(), event.getStatus());

        TransactionView view = viewRepository
            .findById(UUID.fromString(event.getTransactionId()))
            .orElse(TransactionView.builder()
                .id(UUID.fromString(event.getTransactionId()))
                .senderId(event.getSenderId())
                .recipientId(event.getRecipientId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .description(event.getDescription())
                .createdAt(event.getOccurredAt())
                .build());

        view.setStatus(event.getStatus());
        view.setUpdatedAt(LocalDateTime.now());
        viewRepository.save(view);
    }

    // ── Unified query: direction + status ─────────────────────────────

    /**
     * API unificada para consultar transacciones.
     *
     * @param userId    Usuario autenticado
     * @param direction "SENT" | "RECEIVED" | "ALL" (default)
     * @param status    "COMPLETED" | "FAILED" | "PENDING" | "REVERSED" | null (todos)
     * @param page      Página (0-based)
     * @param size      Elementos por página
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(
            String userId,
            String direction,
            String status,
            int page, int size) {

        // Normalize direction
        String dir = (direction == null || direction.isBlank()) ? "ALL" : direction.toUpperCase();
        // Normalize status — null means "all statuses"
        String st  = (status == null || status.isBlank()) ? null : status.toUpperCase();

        Pageable pageable = PageRequest.of(page, size);
        return viewRepository
            .findByDirectionAndStatus(userId, dir, st, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String userId) {
        UserAccount account = accountRepository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new AccountNotFoundException(userId));
        return AccountResponse.builder()
            .userId(account.getUserId())
            .balance(account.getBalance())
            .currency(account.getCurrency())
            .active(account.isActive())
            .build();
    }

    private TransactionResponse toResponse(TransactionView v) {
        return TransactionResponse.builder()
            .id(v.getId().toString())
            .senderId(v.getSenderId())
            .recipientId(v.getRecipientId())
            .amount(v.getAmount())
            .currency(v.getCurrency())
            .description(v.getDescription())
            .status(v.getStatus())
            .failureReason(v.getFailureReason())
            .createdAt(v.getCreatedAt())
            .build();
    }
}

package com.diegoanyosa.payments.command.handler;

import com.diegoanyosa.payments.command.model.*;
import com.diegoanyosa.payments.event.TransactionEvent;
import com.diegoanyosa.payments.exception.*;
import com.diegoanyosa.payments.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS — Command Handler
 *
 * Responsabilidades:
 * 1. Validar el comando (idempotencia, fondos, cuentas válidas)
 * 2. Ejecutar la mutación de estado (débito + crédito atómico)
 * 3. Guardar la transacción en el write model
 * 4. Publicar el evento para que el Query side actualice su proyección
 *
 * NO hace consultas de lectura para el usuario final — eso es del QueryHandler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCommandHandler {

    private final TransactionRepository   transactionRepository;
    private final UserAccountRepository   accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Transaction handle(TransactionCommand command) {

        // ── 1. Idempotencia ───────────────────────────────────
        // Si el commandId ya existe, retornamos la transacción existente
        // sin procesar de nuevo. Clave para reintentos seguros del cliente.
        if (transactionRepository.existsByCommandId(command.getCommandId())) {
            log.info("Duplicate commandId {}, returning existing transaction", command.getCommandId());
            return transactionRepository.findByCommandId(command.getCommandId())
                .orElseThrow(() -> new DuplicateCommandException(command.getCommandId()));
        }

        // ── 2. Seguridad: el que envía debe ser el usuario autenticado ──
        if (!command.getSenderId().equals(command.getRequestedBy())) {
            throw new PaymentException("Sender ID does not match authenticated user");
        }

        // ── 3. Cargar cuentas con bloqueo optimista ───────────
        UserAccount sender = accountRepository
            .findActiveByUserIdForUpdate(command.getSenderId())
            .orElseThrow(() -> new AccountNotFoundException(command.getSenderId()));

        UserAccount recipient = accountRepository
            .findActiveByUserIdForUpdate(command.getRecipientId())
            .orElseThrow(() -> new AccountNotFoundException(command.getRecipientId()));

        // ── 4. Validar moneda ─────────────────────────────────
        if (!sender.getCurrency().equalsIgnoreCase(command.getCurrency())) {
            throw new PaymentException(
                "Account currency " + sender.getCurrency() +
                " does not match transaction currency " + command.getCurrency());
        }

        // ── 5. Validar fondos ─────────────────────────────────
        if (!sender.hasSufficientFunds(command.getAmount())) {
            // Guardar la transacción como FAILED (auditoría)
            Transaction failed = Transaction.builder()
                .commandId(command.getCommandId())
                .senderId(command.getSenderId())
                .recipientId(command.getRecipientId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .description(command.getDescription())
                .status(Transaction.TransactionStatus.FAILED)
                .failureReason("Insufficient funds. Balance: " + sender.getBalance())
                .build();
            transactionRepository.save(failed);
            publishEvent(failed);
            throw new PaymentException("Insufficient funds");
        }

        // ── 6. Ejecutar transferencia (atómico) ───────────────
        sender.debit(command.getAmount());
        recipient.credit(command.getAmount());
        accountRepository.save(sender);
        accountRepository.save(recipient);

        // ── 7. Registrar transacción exitosa ──────────────────
        Transaction tx = Transaction.builder()
            .commandId(command.getCommandId())
            .senderId(command.getSenderId())
            .recipientId(command.getRecipientId())
            .amount(command.getAmount())
            .currency(command.getCurrency())
            .description(command.getDescription())
            .status(Transaction.TransactionStatus.COMPLETED)
            .build();
        transactionRepository.save(tx);

        // ── 8. Publicar evento → Query side lo proyecta ───────
        publishEvent(tx);

        log.info("Transaction {} completed: {} {} from {} to {}",
            tx.getId(), tx.getAmount(), tx.getCurrency(),
            tx.getSenderId(), tx.getRecipientId());

        return tx;
    }

    @Transactional
    public Transaction handleReversal(String transactionId, String requestedBy) {
        Transaction original = transactionRepository.findById(
                java.util.UUID.fromString(transactionId))
            .orElseThrow(() -> new PaymentException("Transaction not found: " + transactionId));

        if (original.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new PaymentException("Only COMPLETED transactions can be reversed");
        }

        // Reverse the funds
        UserAccount sender = accountRepository
            .findActiveByUserIdForUpdate(original.getSenderId())
            .orElseThrow(() -> new AccountNotFoundException(original.getSenderId()));

        UserAccount recipient = accountRepository
            .findActiveByUserIdForUpdate(original.getRecipientId())
            .orElseThrow(() -> new AccountNotFoundException(original.getRecipientId()));

        recipient.debit(original.getAmount());
        sender.credit(original.getAmount());
        accountRepository.save(sender);
        accountRepository.save(recipient);

        // Mark original as REVERSED
        original.setStatus(Transaction.TransactionStatus.REVERSED);
        transactionRepository.save(original);
        publishEvent(original);

        log.info("Transaction {} reversed by {}", transactionId, requestedBy);
        return original;
    }

    private void publishEvent(Transaction tx) {
        TransactionEvent event = TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .transactionId(tx.getId() != null ? tx.getId().toString() : "unknown")
            .senderId(tx.getSenderId())
            .recipientId(tx.getRecipientId())
            .amount(tx.getAmount())
            .currency(tx.getCurrency())
            .status(tx.getStatus().name())
            .description(tx.getDescription())
            .occurredAt(LocalDateTime.now())
            .build();
        eventPublisher.publishEvent(event);
    }
}

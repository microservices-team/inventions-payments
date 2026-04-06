package com.diegoanyosa.payments.controller;

import com.diegoanyosa.payments.command.handler.TransactionCommandHandler;
import com.diegoanyosa.payments.command.model.Transaction;
import com.diegoanyosa.payments.command.model.TransactionCommand;
import com.diegoanyosa.payments.dto.request.CreateTransactionRequest;
import com.diegoanyosa.payments.dto.response.*;
import com.diegoanyosa.payments.query.handler.TransactionQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionCommandHandler commandHandler;
    private final TransactionQueryHandler   queryHandler;

    // ── COMMAND side ────────────────────────────────────────────────────

    /**
     * POST /api/payments/transactions
     * Crea una nueva transferencia usuario → usuario.
     * Body: { commandId, recipientId, amount, currency, description }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest req,
            @RequestHeader("X-User-Id") String userId) {

        TransactionCommand command = TransactionCommand.builder()
            .commandId(req.getCommandId())
            .senderId(userId)
            .recipientId(req.getRecipientId())
            .amount(req.getAmount())
            .currency(req.getCurrency())
            .description(req.getDescription())
            .requestedBy(userId)
            .build();

        Transaction tx = commandHandler.handle(command);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Transaction created", toResponse(tx)));
    }

    /**
     * POST /api/payments/transactions/{id}/reverse
     * Reversa una transacción completada. Solo ADMIN.
     */
    @PostMapping("/{id}/reverse")
    public ResponseEntity<ApiResponse<TransactionResponse>> reverseTransaction(
            @PathVariable String id,
            @RequestHeader("X-User-Id")    String userId,
            @RequestHeader("X-User-Roles") String roles) {

        if (!roles.contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Only ADMIN can reverse transactions"));
        }
        Transaction tx = commandHandler.handleReversal(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction reversed", toResponse(tx)));
    }

    // ── QUERY side ──────────────────────────────────────────────────────

    /**
     * GET /api/payments/transactions
     *
     * API unificada de búsqueda — reemplaza /sent y /received
     *
     * Query params:
     *   direction = SENT | RECEIVED | ALL  (default: ALL)
     *   status    = COMPLETED | FAILED | PENDING | REVERSED  (default: todos)
     *   page      = 0-based  (default: 0)
     *   size      = items per page  (default: 20)
     *
     * Ejemplos:
     *   GET /api/payments/transactions                          → todo el historial
     *   GET /api/payments/transactions?direction=SENT           → solo enviadas
     *   GET /api/payments/transactions?direction=RECEIVED       → solo recibidas
     *   GET /api/payments/transactions?status=FAILED            → solo fallidas
     *   GET /api/payments/transactions?direction=SENT&status=COMPLETED → enviadas completadas
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> search(
            @RequestHeader("X-User-Id")                String userId,
            @RequestParam(required = false)            String direction,
            @RequestParam(required = false)            String status,
            @RequestParam(defaultValue = "0")          int    page,
            @RequestParam(defaultValue = "20")         int    size) {

        Page<TransactionResponse> result = queryHandler.search(userId, direction, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Transactions", result));
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
            .id(tx.getId() != null ? tx.getId().toString() : null)
            .commandId(tx.getCommandId())
            .senderId(tx.getSenderId())
            .recipientId(tx.getRecipientId())
            .amount(tx.getAmount())
            .currency(tx.getCurrency())
            .description(tx.getDescription())
            .status(tx.getStatus().name())
            .failureReason(tx.getFailureReason())
            .createdAt(tx.getCreatedAt())
            .build();
    }
}

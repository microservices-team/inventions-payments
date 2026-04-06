package com.diegoanyosa.payments.controller;

import com.diegoanyosa.payments.command.handler.AccountCommandHandler;
import com.diegoanyosa.payments.dto.request.CreateAccountRequest;
import com.diegoanyosa.payments.dto.response.*;
import com.diegoanyosa.payments.query.handler.TransactionQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandHandler commandHandler;
    private final TransactionQueryHandler queryHandler;

    /** GET — Balance del usuario autenticado */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getMyAccount(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Account info",
            queryHandler.getAccount(userId)));
    }

    /** POST — Crear cuenta (llamado internamente al registrar usuario) */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest req,
            @RequestHeader("X-User-Roles") String roles) {
        if (!roles.contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Only ADMIN can create accounts"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Account created", commandHandler.createAccount(req)));
    }

    /** DELETE — Desactivar cuenta */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<AccountResponse>> deactivateAccount(
            @PathVariable String userId,
            @RequestHeader("X-User-Roles") String roles) {
        if (!roles.contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Only ADMIN can deactivate accounts"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated",
            commandHandler.deactivateAccount(userId)));
    }
}

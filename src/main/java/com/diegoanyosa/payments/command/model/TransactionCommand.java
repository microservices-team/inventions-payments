package com.diegoanyosa.payments.command.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * CQRS — Command Side
 * Representa la intención de realizar una transacción.
 * Inmutable: una vez creado no se modifica.
 */
@Value
@Builder
public class TransactionCommand {
    @NotBlank  String     commandId;     // UUID idempotency key del cliente
    @NotBlank  String     senderId;      // UUID del usuario que envía
    @NotBlank  String     recipientId;   // UUID del usuario que recibe
    @NotNull @Positive
               BigDecimal amount;        // Monto a transferir
    @Size(max = 3)
               String     currency;      // "PEN", "USD", "EUR"
    @Size(max = 255)
               String     description;   // Motivo de la transferencia
    @NotBlank  String     requestedBy;   // userId del JWT (validación seguridad)
}

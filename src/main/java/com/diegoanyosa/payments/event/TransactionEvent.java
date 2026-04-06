package com.diegoanyosa.payments.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CQRS — Domain Event
 * Publicado por el Command Handler tras ejecutar exitosamente.
 * El Query side lo consume para actualizar su proyección (read model).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionEvent {
    private String        eventId;
    private String        transactionId;
    private String        senderId;
    private String        recipientId;
    private BigDecimal    amount;
    private String        currency;
    private String        status;        // COMPLETED, FAILED, REVERSED
    private String        description;
    private LocalDateTime occurredAt;
}

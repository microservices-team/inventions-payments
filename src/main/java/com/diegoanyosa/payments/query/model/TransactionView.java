package com.diegoanyosa.payments.query.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS — Read Model (Query Side)
 * Proyección desnormalizada para consultas rápidas.
 * Se actualiza a partir de los TransactionEvents del Command side.
 * Tiene datos extra (nombres de usuarios) para evitar JOINs en lectura.
 */
@Entity
@Table(name = "transaction_views", schema = "payments",
    indexes = {
        @Index(name = "idx_tv_sender",    columnList = "sender_id"),
        @Index(name = "idx_tv_recipient", columnList = "recipient_id"),
        @Index(name = "idx_tv_status",    columnList = "status"),
        @Index(name = "idx_tv_date",      columnList = "created_at"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionView {

    @Id
    private UUID id;   // Mismo ID que Transaction

    @Column(name = "sender_id",    nullable = false) private String    senderId;
    @Column(name = "sender_name")                    private String    senderName;
    @Column(name = "sender_email")                   private String    senderEmail;

    @Column(name = "recipient_id",    nullable = false) private String recipientId;
    @Column(name = "recipient_name")                    private String recipientName;
    @Column(name = "recipient_email")                   private String recipientEmail;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

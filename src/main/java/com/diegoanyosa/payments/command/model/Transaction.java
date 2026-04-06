package com.diegoanyosa.payments.command.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS — Write Model (Command Side)
 * Tabla de escritura: registra el estado actual de cada transacción.
 * Optimizada para escritura, no para consultas complejas.
 */
@Entity
@Table(name = "transactions", schema = "payments",
    indexes = {
        @Index(name = "idx_tx_sender",     columnList = "sender_id"),
        @Index(name = "idx_tx_recipient",  columnList = "recipient_id"),
        @Index(name = "idx_tx_command_id", columnList = "command_id", unique = true),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Idempotency key — evita duplicar transacciones ante reintentos */
    @Column(name = "command_id", unique = true, nullable = false)
    private String commandId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcType(CharJdbcType.class)
    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** Razón del fallo si status = FAILED */
    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionStatus {
        PENDING,    // Creada, pendiente de procesar
        COMPLETED,  // Procesada exitosamente
        FAILED,     // Falló (fondos insuficientes, cuenta inválida, etc.)
        REVERSED    // Reversada (devolución)
    }
}

package com.diegoanyosa.payments.command.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cuenta bancaria del usuario dentro del módulo de pagos.
 * Separada del auth-service — cada microservicio tiene su propio modelo.
 */
@Entity
@Table(name = "user_accounts", schema = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {

    @Id
    @Column(name = "user_id")
    private String userId;   // UUID del auth-service

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @JdbcType(CharJdbcType.class)
    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "PEN";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version  // Optimistic locking — evita race conditions en concurrencia
    private Long version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!hasSufficientFunds(amount))
            throw new IllegalStateException("Insufficient funds");
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}

package com.diegoanyosa.payments.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    @NotBlank(message = "commandId is required for idempotency")
    private String commandId;

    @NotBlank(message = "recipientId is required")
    private String recipientId;

    @NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @NotBlank @Size(min = 3, max = 3, message = "Currency must be 3 letters (e.g. PEN)")
    private String currency;

    @Size(max = 255)
    private String description;
}

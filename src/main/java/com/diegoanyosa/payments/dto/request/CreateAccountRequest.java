package com.diegoanyosa.payments.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Builder
@Data
public class CreateAccountRequest {
    @NotBlank
    private String userId;

    @DecimalMin("0.00")
    @Builder.Default
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @NotBlank @Size(min = 3, max = 3)
    private String currency;
}

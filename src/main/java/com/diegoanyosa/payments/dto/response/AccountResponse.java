package com.diegoanyosa.payments.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountResponse {
    private String     userId;
    private BigDecimal balance;
    private String     currency;
    private boolean    active;
}

package com.diegoanyosa.payments.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionResponse {
    private String        id;
    private String        commandId;
    private String        senderId;
    private String        recipientId;
    private BigDecimal    amount;
    private String        currency;
    private String        description;
    private String        status;
    private String        failureReason;
    private LocalDateTime createdAt;
}

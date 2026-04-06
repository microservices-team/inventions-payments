package com.diegoanyosa.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PaymentException extends RuntimeException {
    public PaymentException(String message) { super(message); }
}

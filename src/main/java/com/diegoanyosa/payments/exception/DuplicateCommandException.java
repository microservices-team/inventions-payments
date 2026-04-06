package com.diegoanyosa.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCommandException extends RuntimeException {
    public DuplicateCommandException(String commandId) {
        super("Transaction already processed with commandId: " + commandId);
    }
}

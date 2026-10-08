package com.erikjarquin.compras.exceptions;

import org.springframework.http.HttpStatus;

public class EmailDeliveryException extends RuntimeException {
    private final HttpStatus status;
    
    public EmailDeliveryException(String message) {
        this(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }
    
    public EmailDeliveryException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    //Estado de HTTP que debe devolverse al cliente (usa GlobalExceptionHandler para manejarlo)
    public HttpStatus getStatus() {
        return status;
    }
}

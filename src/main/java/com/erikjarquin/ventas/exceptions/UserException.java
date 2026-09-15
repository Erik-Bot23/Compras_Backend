package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de USUARIOS.
 *
 * <p>Por defecto se mapea a 404 NOT_FOUND (mantiene el comportamiento previo).
 * Se puede indicar otro HttpStatus con el segundo constructor.
 */
public class UserException extends RuntimeException {

    private final HttpStatus status;

    public UserException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public UserException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Estado HTTP que debería devolverse al cliente (usa GlobalExceptionHandler).
     */
    public HttpStatus getStatus() {
        return status;
    }
}
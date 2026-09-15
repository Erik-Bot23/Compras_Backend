package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de ROLES.
 *
 * <p>Por defecto se mapea a 404 NOT_FOUND (mantiene el comportamiento previo).
 * Se puede indicar otro HttpStatus con el segundo constructor, p. ej. 409 para
 * "rol con usuarios asignados" o "ya existe un rol con ese nombre".
 */
public class RoleException extends RuntimeException {

    private final HttpStatus status;

    public RoleException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public RoleException(String message, HttpStatus status) {
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
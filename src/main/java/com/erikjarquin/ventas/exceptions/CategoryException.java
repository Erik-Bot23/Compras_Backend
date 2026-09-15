package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de CATEGORÍAS (crear, editar, eliminar).
 *
 * <p>Por defecto se mapea a 404 NOT_FOUND (mantiene el comportamiento previo).
 * Se puede indicar otro HttpStatus con el segundo constructor, p. ej. 409 para
 * "categoría con productos asociados" o "ya existe una categoría con ese nombre".
 */
public class CategoryException extends RuntimeException {

    private final HttpStatus status;

    public CategoryException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public CategoryException(String message, HttpStatus status) {
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
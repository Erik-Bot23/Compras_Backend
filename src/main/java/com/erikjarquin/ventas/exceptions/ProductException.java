package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de PRODUCTOS (catálogo de /api/products).
 *
 * <p>Por defecto 404 NOT_FOUND. Se puede indicar otro HttpStatus con el segundo
 * constructor, p. ej. 409 para "producto con ventas o compras asociadas" (no se
 * puede borrar físicamente porque rompería el histórico).
 */
public class ProductException extends RuntimeException {

    private final HttpStatus status;

    public ProductException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public ProductException(String message, HttpStatus status) {
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
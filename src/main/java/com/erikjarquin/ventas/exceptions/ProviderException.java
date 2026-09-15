package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de PROVEEDORES (CRUD de /api/providers).
 *
 * <p>Por defecto 404 NOT_FOUND. Se puede indicar otro HttpStatus con el segundo
 * constructor: 409 para RFC duplicado o proveedor con compras asociadas, 400
 * para campos inválidos (los lanza el servicio como IllegalArgumentException).
 */
public class ProviderException extends RuntimeException {

    private final HttpStatus status;

    public ProviderException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public ProviderException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
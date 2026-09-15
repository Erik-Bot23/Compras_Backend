package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de COMPRAS (POST/GET/DELETE /api/purchases).
 *
 * <p>Por defecto 404 NOT_FOUND (compra no encontrada). Con el segundo
 * constructor se usan 400 (items vacíos/cantidad o costo inválidos) y 409
 * (proveedor inexistente, inconsistencia al cancelar).
 */
public class PurchaseException extends RuntimeException {

    private final HttpStatus status;

    public PurchaseException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public PurchaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
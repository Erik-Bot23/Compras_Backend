package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de CAJA (apertura, cierre, resumen, caja activa).
 *
 * <p>Por defecto (constructor de un argumento) se mapea a 404 NOT_FOUND para
 * mantener el comportamiento previo, pero se puede indicar un HttpStatus
 * específico con el segundo constructor si el servicio lo requiere
 * (p. ej. 409 para "ya existe una caja abierta").
 */
public class CashException extends RuntimeException {

    private final HttpStatus status;

    public CashException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public CashException(String message, HttpStatus status) {
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
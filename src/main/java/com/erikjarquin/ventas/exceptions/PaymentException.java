package com.erikjarquin.ventas.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de PAGOS.
 *
 * <p>El {@link GlobalExceptionHandler} deduce el HTTP status a partir del texto
 * del mensaje (timeout → 408, problema de terminal → 502, rechazo → 402).
 * Opcionalmente se puede fijar un estado explícito con el segundo constructor
 * para que viaje de forma directa en {@link #getStatus()}.
 */
public class PaymentException extends RuntimeException {

    private final HttpStatus status;

    public PaymentException(String message) {
        this(message, (HttpStatus) null);
    }

    public PaymentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.status = null;
    }

    /**
     * Estado HTTP explícito (puede ser {@code null} → usa la heurística del handler).
     */
    public HttpStatus getStatus() {
        return status;
    }
}
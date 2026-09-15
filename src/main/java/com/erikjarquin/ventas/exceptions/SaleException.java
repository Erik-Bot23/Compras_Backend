package com.erikjarquin.ventas.exceptions;

/**
 * Excepción de negocio del módulo de ventas (stock insuficiente, caja no
 * abierta al vender, método de pago inválido, etc.).
 * El GlobalExceptionHandler la mapea como error 400 BAD_REQUEST.
 */
public class SaleException extends RuntimeException {
    public SaleException(String message){
        super(message);
    }

    public SaleException(String message, Throwable cause){
        super(message, cause);
    }
}

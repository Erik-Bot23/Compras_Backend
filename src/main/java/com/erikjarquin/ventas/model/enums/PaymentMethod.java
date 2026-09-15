package com.erikjarquin.ventas.model.enums;

/**
 * Método de pago de una venta. CASH=efectivo (con vuelto), DEBIT/CREDIT=a
 * través de la terminal de pagos. Se persiste como String en BD.
 */
public enum PaymentMethod {
    CASH,
    DEBIT,
    CREDIT
}

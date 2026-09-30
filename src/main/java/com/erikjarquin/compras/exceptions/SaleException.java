package com.erikjarquin.compras.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio del módulo de ventas.
 *
 * <p>Por defecto 400 BAD_REQUEST, que es lo que devolvía antes de agregarle el
 * HttpStatus: stock insuficiente, caja no abierta al vender, método de pago
 * inválido, venta sin productos, etc. son todos "pedí algo que no se puede
 * cumplir tal cual", no 404.
 *
 * <p>El segundo constructor permite que una misma excepción represente otros
 * códigos cuando la REGLA lo pida. Se usa en el ciclo de vida de la venta
 * (2026-09-30) para devolver 409 CONFLICT en los casos donde el recurso existe
 * pero la operación choca con su estado:
 * <ul>
 *   <li>anular una venta ya confirmada (congelada),</li>
 *   <li>anular una venta ya anulada (devolvería el stock dos veces),</li>
 *   <li>anular una venta con tarjeta (el pago fue capturado: necesita reversa),</li>
 *   <li>confirmar una venta ya anulada.</li>
 * </ul>
 *
 * <p>El patrón es idéntico al de {@code ProductException} / {@code ProviderException}
 * / {@code PurchaseException}: el servicio lanza la excepción con el código
 * desired y {@code GlobalExceptionHandler} solo la traduce. Así el servicio no
 * necesita saber de HTTP y el handler no necesita saber de ventas.
 */
public class SaleException extends RuntimeException {

    private final HttpStatus status;

    /** 400 BAD_REQUEST: el comportamiento por defecto histórico. */
    public SaleException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    /** Código explícito, p. ej. HttpStatus.CONFLICT (409) o NOT_FOUND (404). */
    public SaleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
    }

    /**
     * Estado HTTP que debe devolverse al cliente (lo usa GlobalExceptionHandler).
     */
    public HttpStatus getStatus() {
        return status;
    }
}

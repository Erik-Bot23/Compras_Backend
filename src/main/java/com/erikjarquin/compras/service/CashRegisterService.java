package com.erikjarquin.compras.service;

import java.util.List;

import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.CreateCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;

/**
 * Contrato de la caja registradora.
 *
 * V3 (2026-09-30): la caja se crea antes de abrirse. Antes el único
 * punto de entrada era {@code open()}, que creaba la fila y le ponía el número.
 * Ahora hay dos pasos, y por eso existe {@link #create}:
 *   {@code create} registra la caja física (queda sin abrir).
 *   {@code open} elige una de las registradas y la abre.
 *
 * El motivo es que el número debe existir antes de abrir para poder elegirse
 * de una lista. Ver el javadoc de {@code CashRegisterImpl} para el detalle y
 * para por qué una caja no se puede abrir dos veces.
 *
 * @see com.erikjarquin.compras.service.impl.CashRegisterImpl
 */
public interface CashRegisterService {

    //V3: registra una caja física todavía sin abrir
    CashResponse create(CreateCashRequest request);

    //V3: cajas nunca abiertas, candidatas para abrir
    List<CashResponse> getAvailable();

    //V3: sugiere el siguiente número libre con el patrón "CAJA n"
    String getNextSuggestedNumber();

    //Abre una caja ya registrada (fondo inicial mínimo 100, no negativo)
    CashResponse open(OpenCashRequest request);

    //Cierra la caja exigiendo que el efectivo cuadre (salida de emergencia con motivo)
    CashResponse close(CloseCashRequest request);

    //Ver la caja activa
    CashResponse getActiveCash();

    //Resumen del corte de la caja activa
    CashSummaryResponse getSummary();

    //V3: historial de cajas (la más reciente primero) y detalle por número
    List<CashResponse> getHistory();
    CashResponse getByNumber(String number);
}

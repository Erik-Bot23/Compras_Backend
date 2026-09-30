package com.erikjarquin.compras.service;

import java.util.List;

import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;

/**
 * Contrato de la caja registradora: apertura, cierre, caja activa y resumen.
 * Ver {@code service/impl/CashRegisterImpl}.
 */
public interface CashRegisterService {
    CashResponse open(OpenCashRequest request);
    CashResponse close(CloseCashRequest request);
    CashResponse getActiveCash();
    CashSummaryResponse getSummary();

    //V3: historial de cajas (la mas reciente primero) y detalle por numero
    List<CashResponse> getHistory();
    CashResponse getByNumber(String number);
}

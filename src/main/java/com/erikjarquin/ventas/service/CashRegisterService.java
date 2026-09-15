package com.erikjarquin.ventas.service;

import com.erikjarquin.ventas.model.dto.Cash.CashResponse;
import com.erikjarquin.ventas.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.ventas.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.ventas.model.dto.Cash.OpenCashRequest;

/**
 * Contrato de la caja registradora: apertura, cierre, caja activa y resumen.
 * Ver {@code service/impl/CashRegisterImpl}.
 */
public interface CashRegisterService {
    CashResponse open(OpenCashRequest request);
    CashResponse close(CloseCashRequest request);
    CashResponse getActiveCash();
    CashSummaryResponse getSummary();
}

package com.erikjarquin.compras.service;


import java.util.List;

import com.erikjarquin.compras.model.dto.Sale.SaleDetailHistoryResponse;
import com.erikjarquin.compras.model.dto.Sale.SaleHistoryResponse;
import com.erikjarquin.compras.model.dto.Sale.SaleRequest;
import com.erikjarquin.compras.model.dto.Sale.SaleResponse;

/**
 * Contrato del módulo de ventas: registrar una venta (efectivo o tarjeta),
 * consultar historial y gestionar su ciclo de vida. Ver
 * {@code service/impl/SaleImpl}.
 *
 * <h2>Ciclo de vida (agregado 2026-09-30)</h2>
 *
 * <p>Una venta nace ABIERTA ({@code confirmed=false}, {@code cancelled=false}).
 * Desde ahí tiene exactamente dos salidas y nunca vuelve atrás:
 * <ul>
 *   <li>{@link #confirm} → CONGELADA. Irreversible: ni anular ni borrar.</li>
 *   <li>{@link #cancel}  → ANULADA. El stock vuelve al inventario y la fila se
 *       conserva (auditoría). Sólo efectivo y sólo si no está confirmada.</li>
 * </ul>
 *
 * <p>Por qué existe: si se pudiera anular una venta consumida por el cliente, el
 * inventario volvería a contar mercadería que ya salió, y en cascada eso
 * permitiría cancelar una COMPRA cuyo stock ya se vendió, dejando el almacén
 * con números que no corresponden a la realidad (ver {@code PurchaseImpl.cancel}).
 */
public interface SaleService {
    SaleResponse processSale(SaleRequest request);
    List<SaleHistoryResponse> getSales(); 
    SaleDetailHistoryResponse getSaleById(Long saleId);

    /**
     * Confirma (congela) la venta. Idempotente: confirmar dos veces no falla,
     * devuelve la misma venta.
     *
     * @throws SaleException 404 si no existe, 409 si ya estaba anulada.
     */
    SaleHistoryResponse confirm(Long saleId);

    /**
     * Anula la venta y devuelve el stock al inventario.
     *
     * @throws SaleException 404 si no existe, 409 si ya está confirmada, si ya
     *         estaba anulada, o si el pago fue con tarjeta (requiere reversa real
     *         del pago vía {@code POST /api/local/payments/reverse/{id}}).
     */
    SaleHistoryResponse cancel(Long saleId);
}

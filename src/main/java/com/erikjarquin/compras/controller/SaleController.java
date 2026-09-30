package com.erikjarquin.compras.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.compras.model.dto.Sale.SaleDetailHistoryResponse;
import com.erikjarquin.compras.model.dto.Sale.SaleHistoryResponse;
import com.erikjarquin.compras.model.dto.Sale.SaleRequest;
import com.erikjarquin.compras.model.dto.Sale.SaleResponse;
import com.erikjarquin.compras.service.SaleService;

/**
 * Ventas: registrar una venta (efectivo o tarjeta), consultar el historial y
 * gestionar su ciclo de vida (confirmar / anular).
 *
 * <p>Permisos por operación:
 * <ul>
 *   <li>{@code CREAR_VENTAS}    → {@code POST} (cobrar)</li>
 *   <li>{@code VER_VENTAS}      → {@code GET} lista y detalle</li>
 *   <li>{@code CONFIRMAR_VENTAS}→ {@code PATCH /{id}/confirm} (cerrar, congela)</li>
 *   <li>{@code CANCELAR_VENTAS} → {@code PATCH /{id}/cancel} (anular, devuelve stock)</li>
 * </ul>
 *
 * <p>No existe {@code DELETE /{id}} a propósito: las ventas no se borran
 * nunca. Se anulan (quedan con {@code cancelled = true}) para que el historial
 * siga siendo auditable.
 *
 * <p>CORS global en SecurityConfig (${CORS_ALLOWED_ORIGINS}), sin
 * {@code @CrossOrigin} aquí.
 */
@RestController
@RequestMapping("/api/local/sales")
public class SaleController {
    private final SaleService service;

    public SaleController(SaleService service){
        this.service=service;
    }

    //Realizar la venta
    @PreAuthorize("hasAuthority('CREAR_VENTAS')")
    @PostMapping
    public ResponseEntity<SaleResponse> processSale(@RequestBody SaleRequest request){
        SaleResponse response = service.processSale(request);
        return ResponseEntity.ok(response);
    }

    //Ver los detalles de la venta
    @PreAuthorize("hasAuthority('VER_VENTAS')")
    @GetMapping
    public List<SaleHistoryResponse> getSales(){
        return service.getSales();
    }

    //Ver venta por ID
    @PreAuthorize("hasAuthority('VER_VENTAS')")
    @GetMapping("/{id}")
    public SaleDetailHistoryResponse getSaleById(@PathVariable Long id){
        return service.getSaleById(id);
    }

    /**
     * CONFIRMAR una venta (la congela para siempre).
     *
     * <p>Se ejecuta cuando el pedido ya salió del mostrador. A partir de aquí
     * la venta no se puede anular ni borrar jamás.
     *
     * <p><b>Por qué PATCH y no POST ni PUT:</b> PATCH significa "cambio
     * parcial": no se reenvía la venta entera, solo se le pide mover su estado
     * a confirmado. Es la semántica correcta y además evita el problema clásico
     * de un PUT que reenvía campos viejos y pisa datos que otro usuario cambió.
     * Tampoco se usa POST porque la venta ya existe: no se crea nada.
     *
     * <p><b>Permiso propio ({@code CONFIRMAR_VENTAS}) y no {@code CREAR_VENTAS}:</b>
     * cobrar y "dar por salida la comida" son responsabilidades distintas. En un
     * POS real quien cobra (caja) y quien entrega (cocina/mostrador) suelen ser
     * personas distintas; con un permiso único cualquiera podría cerrar la
     * venta de otro y dejarlo sin poder corregirla.
     *
     * <p>Devuelve 200 con la venta actualizada. Si ya estaba confirmada también
     * devuelve 200 (idempotencia); los 409 son para el caso incompatible de
     * verdad, que es intentar confirmar una venta ya anulada.
     */
    @PreAuthorize("hasAuthority('CONFIRMAR_VENTAS')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<SaleHistoryResponse> confirm(@PathVariable Long id){
        return ResponseEntity.ok(service.confirm(id));
    }

    /**
     * ANULAR una venta: devuelve el stock al inventario y conserva el registro
     * con {@code cancelled = true}.
     *
     * <p>No borra la fila. En un POS el historial es evidencia contable: una
     * venta anulada que desaparece deja el mismo hueco en los números que una
     * venta que nunca existió, y no hay forma de auditar quién la quitó.
     *
     * <p>Rechazos (409) que devuelve el servicio, no el controller:
     * <ul>
     *   <li>la venta ya está confirmada (congelada),</li>
     *   <li>la venta ya estaba anulada (el stock ya volvió una vez),</li>
     *   <li>la venta fue con tarjeta: el pago está capturado y necesita una
     *       reversa real vía {@code POST /api/local/payments/reverse/{id}}.</li>
     * </ul>
     *
     * <p>Permiso {@code CANCELAR_VENTAS} para que un cajero con
     * {@code CREAR_VENTAS} no pueda deshacer ventas por su cuenta.
     */
    @PreAuthorize("hasAuthority('CANCELAR_VENTAS')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SaleHistoryResponse> cancel(@PathVariable Long id){
        return ResponseEntity.ok(service.cancel(id));
    }
}

package com.erikjarquin.ventas.service;

import java.util.List;

import com.erikjarquin.ventas.model.dto.Purchases.PurchaseDTO;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseRequest;

/**
 * Contrato del módulo de COMPRAS (registro de mercancía y su detalle).
 * Ver {@code service/impl/PurchaseImpl}.
 */
public interface PurchaseService {
    //Registrar una compra: suma stock y actualiza el costo del producto
    PurchaseDTO create(PurchaseRequest request);

    //Listado completo de compras (más recientes primero)
    List<PurchaseDTO> getAll();

    //Compras de un proveedor concreto
    List<PurchaseDTO> getByProvider(Long providerId);

    //Detalle de una compra (con sus renglones)
    PurchaseDTO getById(Long id);

    //Cancelar una compra: revierte el stock de sus productos y la elimina
    void cancel(Long id);
}
package com.erikjarquin.ventas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Purchases.PurchaseDTO;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseRequest;
import com.erikjarquin.ventas.service.PurchaseService;

/**
 * Módulo de COMPRAS: registro de mercancía comprada a proveedores.
 *
 * Público dentro del sistema (autenticado), protegido por permiso:
 *  - VER_COMPRAS        → GET (todos, por proveedor y detalle)
 *  - CREAR_COMPRAS      → POST (suma stock y guarda el costo real)
 *  - CANCELAR_COMPRAS   → DELETE /{id} (revierte stock)
 */
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService){
        this.purchaseService = purchaseService;
    }

    //Listado de todas las compras (más recientes primero)
    @PreAuthorize("hasAuthority('VER_COMPRAS')")
    @GetMapping
    public List<PurchaseDTO> getAll(){
        return purchaseService.getAll();
    }

    //Detalle de una compra (renglones + producto)
    @PreAuthorize("hasAuthority('VER_COMPRAS')")
    @GetMapping("/{id}")
    public PurchaseDTO getById(@PathVariable Long id){
        return purchaseService.getById(id);
    }

    //Compras de un proveedor concreto (?providerId=)
    @PreAuthorize("hasAuthority('VER_COMPRAS')")
    @GetMapping("/provider/{providerId}")
    public List<PurchaseDTO> getByProvider(@PathVariable Long providerId){
        return purchaseService.getByProvider(providerId);
    }

    //Registrar una compra
    @PreAuthorize("hasAuthority('CREAR_COMPRAS')")
    @PostMapping
    public ResponseEntity<PurchaseDTO> create(@RequestBody PurchaseRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(request));
    }

    //Cancelar una compra (revierte el stock de sus productos)
    @PreAuthorize("hasAuthority('CANCELAR_COMPRAS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id){
        purchaseService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
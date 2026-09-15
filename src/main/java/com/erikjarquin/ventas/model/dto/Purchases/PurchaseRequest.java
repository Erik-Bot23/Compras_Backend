package com.erikjarquin.ventas.model.dto.Purchases;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Petición de registro de COMPRA (POST /api/purchases).
 * providerId es obligatorio. purchaseDate es opcional (si no llega se usa
 * la fecha/hora actual del servidor). items debe tener al menos 1 renglón.
 */
public class PurchaseRequest {
    private Long providerId;
    private LocalDateTime purchaseDate;
    private List<PurchaseItemRequest> items;

    public PurchaseRequest(){}

    public Long getProviderId(){ return providerId; }
    public void setProviderId(Long providerId){ this.providerId=providerId; }

    public LocalDateTime getPurchaseDate(){ return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate){ this.purchaseDate=purchaseDate; }

    public List<PurchaseItemRequest> getItems(){ return items; }
    public void setItems(List<PurchaseItemRequest> items){ this.items=items; }
}
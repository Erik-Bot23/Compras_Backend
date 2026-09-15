package com.erikjarquin.ventas.model.dto.Purchases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de COMPRA (salida de GET /api/purchases y POST /api/purchases).
 * providerName se calcula en el mapper para los listados; totalItems es la
 * cantidad de renglones (no unidades).
 */
public class PurchaseDTO {
    private Long id;
    private LocalDateTime purchaseDate;
    private Long providerId;
    private String providerName;
    private BigDecimal total;
    private Integer totalItems;
    private List<PurchaseItemDTO> items;

    public PurchaseDTO(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public LocalDateTime getPurchaseDate(){ return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate){ this.purchaseDate=purchaseDate; }

    public Long getProviderId(){ return providerId; }
    public void setProviderId(Long providerId){ this.providerId=providerId; }

    public String getProviderName(){ return providerName; }
    public void setProviderName(String providerName){ this.providerName=providerName; }

    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total=total; }

    public Integer getTotalItems(){ return totalItems; }
    public void setTotalItems(Integer totalItems){ this.totalItems=totalItems; }

    public List<PurchaseItemDTO> getItems(){ return items; }
    public void setItems(List<PurchaseItemDTO> items){ this.items=items; }
}
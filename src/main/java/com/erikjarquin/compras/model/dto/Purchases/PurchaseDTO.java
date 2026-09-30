package com.erikjarquin.compras.model.dto.Purchases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de COMPRA (salida de GET /api/local/purchases y POST /api/local/purchases).
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

    /**
     * Estado del ciclo de vida de la compra (V3). false = PENDIENTE (registrada
     * pero sin efecto en el inventario, cancelable); true = CONFIRMADA (el stock
     * ya sumó y el costo ya se guardó, congelada).
     */
    private boolean confirmed;
    private LocalDateTime confirmedAt;

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

    //Getter y setter de confirmed (V3)
    public boolean isConfirmed(){ return confirmed; }
    public void setConfirmed(boolean confirmed){ this.confirmed=confirmed; }

    //Getter y setter de confirmedAt (V3)
    public LocalDateTime getConfirmedAt(){ return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt){ this.confirmedAt=confirmedAt; }
}
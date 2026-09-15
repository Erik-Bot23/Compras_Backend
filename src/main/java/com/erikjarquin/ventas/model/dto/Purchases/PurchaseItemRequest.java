package com.erikjarquin.ventas.model.dto.Purchases;

import java.math.BigDecimal;

/**
 * Item de compra que llega del cliente (dentro de PurchaseRequest).
 * productId identifica el producto, quantity las unidades y unitCost el costo
 * unitario REAL pagado al proveedor (se copia al producto como su costo).
 */
public class PurchaseItemRequest {
    private Long productId;
    private Integer quantity;
    private BigDecimal unitCost;

    public PurchaseItemRequest(){}

    public Long getProductId(){ return productId; }
    public void setProductId(Long productId){ this.productId=productId; }

    public Integer getQuantity(){ return quantity; }
    public void setQuantity(Integer quantity){ this.quantity=quantity; }

    public BigDecimal getUnitCost(){ return unitCost; }
    public void setUnitCost(BigDecimal unitCost){ this.unitCost=unitCost; }
}
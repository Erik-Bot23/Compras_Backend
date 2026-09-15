package com.erikjarquin.ventas.model.dto.Reports;

import java.math.BigDecimal;

/**
 * Producto más vendido ({@code GET /api/reports/top-products}).
 * {@code quantity} es la suma de unidades vendidas y {@code total} la suma en
 * dinero (usando el precio de VENTA ya sumado en sale_details).
 */
public class TopProductDTO {
    private Long productId;
    private String name;
    private Long quantity;
    private BigDecimal total;

    public TopProductDTO(){}

    public TopProductDTO(Long productId, String name, Long quantity, BigDecimal total){
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.total = total;
    }

    public Long getProductId(){ return productId; }
    public void setProductId(Long productId){ this.productId = productId; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }

    public Long getQuantity(){ return quantity; }
    public void setQuantity(Long quantity){ this.quantity = quantity; }

    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total = total; }
}
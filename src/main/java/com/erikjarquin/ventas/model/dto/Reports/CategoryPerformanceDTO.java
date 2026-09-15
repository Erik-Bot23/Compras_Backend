package com.erikjarquin.ventas.model.dto.Reports;

import java.math.BigDecimal;

/**
 * Rendimiento por categoría ({@code GET /api/reports/categories}).
 * Suma unidades ({@code quantity}) y ventas ({@code total}) agrupando los
 * detalles de venta por la categoría de cada producto. Los productos sin
 * categoría se agrupan bajo "Sin categoría".
 */
public class CategoryPerformanceDTO {
    private Long categoryId;
    private String name;
    private Long quantity;
    private BigDecimal total;

    public CategoryPerformanceDTO(){}

    public CategoryPerformanceDTO(Long categoryId, String name, Long quantity, BigDecimal total){
        this.categoryId = categoryId;
        this.name = name;
        this.quantity = quantity;
        this.total = total;
    }

    public Long getCategoryId(){ return categoryId; }
    public void setCategoryId(Long categoryId){ this.categoryId = categoryId; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }

    public Long getQuantity(){ return quantity; }
    public void setQuantity(Long quantity){ this.quantity = quantity; }

    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total = total; }
}
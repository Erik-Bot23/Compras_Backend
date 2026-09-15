package com.erikjarquin.ventas.model.dto.Reports;

/**
 * Producto con stock bajo o agotado ({@code GET /api/reports/low-stock}).
 * Se devuelve el stock real de inventario (no el histórico de ventas), para
 * decidir reposiciones.
 */
public class LowStockDTO {
    private Long productId;
    private String name;
    private String sku;
    private String barcode;
    private String category;
    private Integer stock;

    public LowStockDTO(){}

    public LowStockDTO(Long productId, String name, String sku, String barcode, String category, Integer stock){
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.barcode = barcode;
        this.category = category;
        this.stock = stock;
    }

    public Long getProductId(){ return productId; }
    public void setProductId(Long productId){ this.productId = productId; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }

    public String getSku(){ return sku; }
    public void setSku(String sku){ this.sku = sku; }

    public String getBarcode(){ return barcode; }
    public void setBarcode(String barcode){ this.barcode = barcode; }

    public String getCategory(){ return category; }
    public void setCategory(String category){ this.category = category; }

    public Integer getStock(){ return stock; }
    public void setStock(Integer stock){ this.stock = stock; }
}
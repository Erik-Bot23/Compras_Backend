package com.erikjarquin.ventas.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad {@code products}: artículos del inventario para la venta.
 *
 * <p>{@code img} guarda el NOMBRE del archivo en disco (UUID+ext), no la URL;
 * la URL completa la construye ProductMapper con ${app.upload-url}.
 * barcode y sku son únicos; stock es la cantidad física disponible.
 */
@Entity
@Table(name="products")
public class ProductEntity {
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    /**
     * Costo unitario REAL del producto, alimentado por el módulo de COMPRAS:
     * al registrar una compra se guarda el último unitCost recibido. 0 cuando
     * nunca se ha comprado. price - cost = margen (estadística de Márgenes).
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(nullable = false)
    private int stock;

    @ManyToOne(fetch=FetchType.LAZY)//No genera posibles consultas dobles, una por cada producto
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    private String img;

    @Column(unique = true)
    private String barcode;

    @Column(unique = true)
    private String sku;

    public ProductEntity(){}

    //getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //getter y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    //getter y setter de price
    public BigDecimal getPrice(){
        return price;
    }

    public void setPrice(BigDecimal price){
        this.price=price;
    }

    //Getter y setter de cost
    public BigDecimal getCost(){
        return cost;
    }

    public void setCost(BigDecimal cost){
        this.cost=cost;
    }

    //getter y setter de stock
    public int getStock(){
        return stock;
    }

    public void setStock(int stock){
        this.stock=stock;
    }

    //getter y setter de category
    public CategoryEntity getCategory(){
        return category;
    }

    public void setCategory(CategoryEntity category){
        this.category=category;
    }

    //getter y setter de img
    public String getImg(){
        return img;
    }

    public void setImg(String img){
        this.img=img;
    }

    //Getter y setter de barcode
    public String getBarcode(){
        return barcode;
    }

    public void setBarcode(String barcode){
        this.barcode=barcode;
    }

    //Getter y setter de sku
    public String getSku(){
        return sku;
    }

    public void setSku(String sku){
        this.sku=sku;
    }
}


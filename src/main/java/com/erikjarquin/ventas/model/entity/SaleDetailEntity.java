package com.erikjarquin.ventas.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad {@code sale_details}: renglón de venta (un producto vendido).
 *
 * <p>Guarda una copia del precio unitario en el momento de la venta (unitPrice)
 * para que cambios futuros de precio no alteren el histórico. subtotal =
 * unitPrice * quantity.
 */
@Entity
@Table(name = "sale_details")
public class SaleDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    private SaleEntity sale;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    public SaleDetailEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de sale
    public SaleEntity getSale(){
        return sale;
    }

    public void setSale(SaleEntity sale){
        this.sale=sale;
    }

    //Getter y setter de product
    public ProductEntity getProduct(){
        return product;
    }

    public void setProduct(ProductEntity product){
        this.product=product;
    }

    //Getter y setter de quantity
    public Integer getQuantity(){
        return quantity;
    }

    public void setQuantity(Integer quantity){
        this.quantity=quantity;
    }

    //Getter y setter de unitPrice
    public BigDecimal getUnitPrice(){
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice){
        this.unitPrice=unitPrice;
    }

    //Getter y setter de subtotal
    public BigDecimal getSubTotal(){
        return subtotal;
    }

    public void setSubTotal(BigDecimal subtotal){
        this.subtotal=subtotal;
    }


}

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
 * Entidad {@code purchase_details}: renglón de compra (un producto adquirido).
 *
 * <p>Guarda una copia del COSTO unitario en el momento de la compra (unitCost)
 * para que el histórico no cambie aunque después se vuelva a comprar más caro
 * o barato. subtotal = unitCost * quantity. El servicio usa ei unitCost de la
 * compra para actualizar product.cost (el costo real vigente del producto).
 */
@Entity
@Table(name = "purchase_details")
public class PurchaseDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private PurchaseEntity purchase;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    private Integer quantity;

    private BigDecimal unitCost;

    private BigDecimal subtotal;

    public PurchaseDetailEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de purchase
    public PurchaseEntity getPurchase(){
        return purchase;
    }

    public void setPurchase(PurchaseEntity purchase){
        this.purchase=purchase;
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

    //Getter y setter de unitCost
    public BigDecimal getUnitCost(){
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost){
        this.unitCost=unitCost;
    }

    //Getter y setter de subtotal
    public BigDecimal getSubtotal(){
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal){
        this.subtotal=subtotal;
    }
}
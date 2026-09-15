package com.erikjarquin.ventas.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidad {@code purchases}: cabecera de una compra a un proveedor.
 *
 * <p>Relaciones: 1:N a PurchaseDetailEntity (artículos comprados, cascade ALL +
 * orphanRemoval, mismo patrón que sales/details) y N:1 a ProviderEntity (quién
 * nos vendió). total = Σ unitCost * quantity de los detalles.
 *
 * <p>Efectos secundarios al registrar/cancelar (transaccionales, en el
 * servicio): la compra SUMA stock del producto y guarda su costo real; la
 * cancelación lo revierte.
 */
@Entity
@Table(name = "purchases")
public class PurchaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime purchaseDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @OneToMany(
        mappedBy = "purchase",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<PurchaseDetailEntity> details;

    private BigDecimal total;

    public PurchaseEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de purchaseDate
    public LocalDateTime getPurchaseDate(){
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate){
        this.purchaseDate=purchaseDate;
    }

    //Getter y setter de provider
    public ProviderEntity getProvider(){
        return provider;
    }

    public void setProvider(ProviderEntity provider){
        this.provider=provider;
    }

    //Getter y setter de details
    public List<PurchaseDetailEntity> getDetails(){
        return details;
    }

    public void setDetails(List<PurchaseDetailEntity> details){
        this.details=details;
    }

    //Getter y setter de total
    public BigDecimal getTotal(){
        return total;
    }

    public void setTotal(BigDecimal total){
        this.total=total;
    }
}
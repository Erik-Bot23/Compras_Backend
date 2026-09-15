package com.erikjarquin.ventas.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.erikjarquin.ventas.model.enums.PaymentMethod;
import com.erikjarquin.ventas.model.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entidad {@code sales}: cabecera de una venta.
 *
 * <p>Relaciones: 1:N a SaleDetailEntity (productos vendidos, cascade ALL +
 * orphanRemoval), N:1 a CashRegisterEntity (caja donde se registró) y 1:1 a
 * PaymentEntity (solo si fue pagada con tarjeta). paymentStatus recorre:
 * PENDING → APPROVED | REJECTED | REVERSED.
 */
@Entity
@Table(name = "sales")
public class SaleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime saleDate;

    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal cashReceived;

    private BigDecimal changeAmount;

    @OneToMany(
        mappedBy = "sale",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<SaleDetailEntity> details;

    @ManyToOne
    @JoinColumn(name = "cash_register_id")
    private CashRegisterEntity cashRegister;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentEntity payment;

    public SaleEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de saleDate
    public LocalDateTime getSaleDate(){
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate){
        this.saleDate=saleDate;
    }

    //Getter y setter de total
    public BigDecimal getTotal(){
        return total;
    }

    public void setTotal(BigDecimal total){
        this.total=total;
    }

    //Getter y setter de paymentMethod
    public PaymentMethod getPaymentMethod(){
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod){
        this.paymentMethod=paymentMethod;
    }

    //Getter y setter de cashReceived
    public BigDecimal getCashReceived(){
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived){
        this.cashReceived=cashReceived;
    }

    //Getter y setter de changeAmount
    public BigDecimal getChangeAmount(){
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount){
        this.changeAmount=changeAmount;
    }

    //Getter y setter de details
    public List<SaleDetailEntity> getDetails(){
        return details;
    }

    public void setDetails(List<SaleDetailEntity> details){
        this.details=details;
    }

    //Getter y setter de CashRegister
    public CashRegisterEntity getCashRegister(){
        return cashRegister;
    }

    public void setCashRegister(CashRegisterEntity cashRegister){
        this.cashRegister=cashRegister;
    }

    //Getter y setter de paymentStatus
    public PaymentStatus getPaymentStatus(){
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus){
        this.paymentStatus=paymentStatus;
    }

    //Getter y setter de payment
    public PaymentEntity getPayment(){
        return payment;
    }

    public void setPayment(PaymentEntity payment){
        this.payment=payment;
    }

}

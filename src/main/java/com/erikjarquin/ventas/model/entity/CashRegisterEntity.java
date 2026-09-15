package com.erikjarquin.ventas.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad {@code cash_registers}: registro de apertura/cierre de caja.
 *
 * <p>active=true indica la caja vigente (solo una a la vez). Durante el cierre
 * se congela el "corte": expectedAmount (fondo inicial + ventas en efectivo),
 * difference (contado - esperado) y los totales por método de pago.
 *
 * <p>Nota: el usuario que abre la caja NO se registra en esta versión (la
 * relación a User está planeada, por eso la entidad no tiene FK a users).
 */
@Entity
@Table(name = "cash_registers")
public class CashRegisterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private BigDecimal openingAmount;

    private BigDecimal countedAmount;

    private Boolean active;

    //Nuevos campos para corte de caja
    private BigDecimal expectedAmount;

    private BigDecimal difference;

    private BigDecimal cashSales;

    private BigDecimal debitSales;

    private BigDecimal creditSales;

    private BigDecimal totalSales;

    private int totalTickets;

    public CashRegisterEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de openedAt
    public LocalDateTime getOpenedAt(){
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt){
        this.openedAt=openedAt;
    }

    //Getter y setter de closedAt
    public LocalDateTime getClosedAt(){
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt){
        this.closedAt=closedAt;
    }
    
    //Getter y setter de openingAmount
    public BigDecimal getOpeningAmount(){
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount){
        this.openingAmount=openingAmount;
    }

    //Getter y setter de countedAmount
    public BigDecimal getCountedAmount(){
        return countedAmount;
    }

    public void setCountedAmount(BigDecimal countedAmount){
        this.countedAmount=countedAmount;
    }

    //Getter y setter de active 
    public Boolean getActive(){
        return active;
    }

    public void setActive(Boolean active){
        this.active=active;
    }

    //Getter y setter de expectedAmount 
    public BigDecimal getExpectedAmount(){
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount){
        this.expectedAmount=expectedAmount;
    }

    //Getter y setter de difference 
    public BigDecimal getDifference(){
        return difference;
    }

    public void setDifference(BigDecimal difference){
        this.difference = difference;
    }

    //Getter y setter de cashSales 
    public BigDecimal getCashSales(){
        return cashSales;
    }

    public void setCashSales(BigDecimal cashSales){
        this.cashSales=cashSales;
    }

    //Getter y setter de debitSales 
    public BigDecimal getDebitSales(){
        return debitSales;
    }

    public void setDebitSales(BigDecimal debitSales){
        this.debitSales=debitSales;
    }

    //Getter y setter de creditSales 
    public BigDecimal getCreditSales(){
        return creditSales;
    }

    public void setCreditSales(BigDecimal creditSales){
        this.creditSales=creditSales;
    }

    //Getter y setter de totalSales 
    public BigDecimal getTotalSales(){
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales){
        this.totalSales=totalSales;
    }

    //Getter y setter de totalTickets 
    public int getTotalTickets(){
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets){
        this.totalTickets=totalTickets;
    }
}

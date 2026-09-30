package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//DTO de respuesta de caja
public class CashResponse {
    private Long id;

    /**
     * Numero de la caja (V3). Es lo que se muestra en la lista de cajas de
     * Reportes y lo que el usuario elige para filtrar.
     */
    private String number;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingAmount;
    private BigDecimal closingAmount;
    private Boolean active;
    private BigDecimal expectedAmount;
    private BigDecimal difference;
    private BigDecimal cashSales;
    private BigDecimal debitSales;
    private BigDecimal creditSales;
    private BigDecimal totalSales;
    private int totalTickets;

    //Constructor vacío
    public CashResponse(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

public void setId(Long id){
        this.id = id;
    }

    //Getter y setter de number
    public String getNumber(){
        return number;
    }

    public void setNumber(String number){
        this.number = number;
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

    //Getter y setter de closingAmount
    public BigDecimal getClosingAmount(){
        return closingAmount;
    }

    public void setClosingAmount(BigDecimal closingAmount){
        this.closingAmount=closingAmount;
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

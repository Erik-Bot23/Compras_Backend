package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;

//DTO de respuesta del resumen de caja
public class CashSummaryResponse {
    private Long cashId;
    private BigDecimal openingAmount;
    private BigDecimal cashSales;
    private BigDecimal debitSales;
    private BigDecimal creditSales;
    private BigDecimal totalSales;
    private BigDecimal expectedAmount;
    private int totalTickets;

    //Constructor vacío
    public CashSummaryResponse(){}

    //Getter y setter de cashId
    public Long getCashId(){
        return cashId;
    }

    public void setCashId(Long cashId){
        this.cashId=cashId;
    }

    //Getter y setter de openingAmount
    public BigDecimal getOpeningAmount(){
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount){
        this.openingAmount=openingAmount;
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

    //Getter y setter de expectedAmount
    public BigDecimal getExpectedAmount(){
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount){
        this.expectedAmount=expectedAmount;
    }

    //Getter y setter de totalTickets
    public int getTotalTickets(){
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets){
        this.totalTickets=totalTickets;
    }
}

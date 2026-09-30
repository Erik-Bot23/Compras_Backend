package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;

//DTO de apertura de caja
public class OpenCashRequest {
    private BigDecimal openingAmount;

    /**
     * Numero que escribe el vendedor al abrir la caja (V3). Obligatorio y
     * UNIQUE en la BD: es lo que despues permite filtrar por caja en Reportes.
     */
    private String number;

    //Constructor vacío
    public OpenCashRequest(){}

    //Getter y setter de openingAmount
    public BigDecimal getOpeningAmount(){
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount){
        this.openingAmount = openingAmount;
    }

    //Getter y setter de number
    public String getNumber(){
        return number;
    }

    public void setNumber(String number){
        this.number = number;
    }
}

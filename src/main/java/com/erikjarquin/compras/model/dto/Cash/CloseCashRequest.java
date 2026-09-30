package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;

//DTO de pedido de caja cerrado
public class CloseCashRequest {
    private BigDecimal closingAmount;

    //Constructor vacío
    public CloseCashRequest(){}

    //Getter y setter de closingAmount
    public BigDecimal getClosingAmount(){
        return closingAmount;
    }

    public void setClosingAmount(BigDecimal closingAmount){
        this.closingAmount=closingAmount;
    }
}

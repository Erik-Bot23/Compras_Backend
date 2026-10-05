package com.erikjarquin.compras.model.dto.Cash;

public class CashBoxRequest {
    //Número de la caja (obligatorio)
    private String number;

    private String description;

    public CashBoxRequest(){}

    //Getter y setter de number
    public String getNumber(){
        return number;
    }

    public void setNumber(String number){
        this.number=number;
    }
    
    //Getter y setter de description
    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description=description;
    }
}

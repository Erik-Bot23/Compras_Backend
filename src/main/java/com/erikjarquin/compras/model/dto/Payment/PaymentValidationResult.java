package com.erikjarquin.compras.model.dto.Payment;

import lombok.Data;
import lombok.NoArgsConstructor;

//DTO de validación del resultado del pago
@Data //
@NoArgsConstructor //
public class PaymentValidationResult {
    private boolean valid;
    private String errorMessage; 
    
    //Constructor
    public PaymentValidationResult(boolean valid, String errorMessage){
        this.valid=valid;
        this.errorMessage=errorMessage;
    }
    
    //Getter y setter de valid
    public boolean isValid(){
        return valid;
    }

    public void setValid(boolean valid){
        this.valid=valid;
    }

    //Getter y setter de errorMessage
    public String getErrorMessage(){
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage){
        this.errorMessage=errorMessage;
    }
}

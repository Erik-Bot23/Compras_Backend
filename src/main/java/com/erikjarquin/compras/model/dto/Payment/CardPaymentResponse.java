package com.erikjarquin.compras.model.dto.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erikjarquin.compras.model.enums.PaymentStatus;

//DTO de respuesta de pago con tarjeta
public class CardPaymentResponse {
    private Long paymentId;
    private Long saleId;
    private PaymentStatus status;
    private String authorizationCode;
    private String message;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String transactionId;
    private String lastFourDigits; // Últimos 4 digitos de la tarjeta

    //Getter y setter de paymentId
    public Long getPaymentId(){
        return paymentId;
    }

    public void setPaymentId(Long paymentId){
        this.paymentId=paymentId;
    }

    //Getter y setter de saleId
    public Long getSaleId(){
        return saleId;
    }

    public void setSaleId(Long saleId){
        this.saleId=saleId;
    }

    //Getter y setter de status
    public PaymentStatus getStatus(){
        return status;
    }

    public void setStatus(PaymentStatus status){
        this.status=status;
    }

    //Getter y setter de authorizationCode
    public String getAuthorizationCode(){
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode){
        this.authorizationCode=authorizationCode;
    }

    //Getter y setter de message
    public String getMessage(){
        return message;
    }

    public void setMessage(String message){
        this.message=message;
    }

    //Getter y setter de amount
    public BigDecimal getAmount(){
        return amount;
    }

    public void setAmount(BigDecimal amount){
        this.amount=amount;
    }

    //Getter y setter de paymentDate
    public LocalDateTime getPaymentDate(){
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate){
        this.paymentDate=paymentDate;
    }

    //Getter y setter de transactionId
    public String getTransactionId(){
        return transactionId;
    }

    public void setTransactionId(String transactionId){
        this.transactionId=transactionId;
    }

    //Getter y setter de lastFourDigits
    public String getLastFourDigits(){
        return lastFourDigits;
    }

    public void setLastFourDigits(String lasrFourDigits){
        this.lastFourDigits=lasrFourDigits;
    }
}

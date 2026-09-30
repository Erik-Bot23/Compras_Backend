package com.erikjarquin.compras.model.dto.Sale;

import java.math.BigDecimal;

import com.erikjarquin.compras.model.dto.Payment.CardPaymentResponse;
import com.erikjarquin.compras.model.enums.PaymentMethod;
import com.erikjarquin.compras.model.enums.PaymentStatus;

public class SaleResponse {
    private Long saleId;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private BigDecimal cashReceived;
    private BigDecimal changeAmount;
    private PaymentStatus paymentStatus; // Para sabber si fue aprobado
    private String lastFourDigits; // Últimos 4 digitos de la tarjeta
    private String authorizationCode; // Código de autorización
    private String erroMessage; // Si fue rechazado
    private CardPaymentResponse cardPaymentResponse;

    public SaleResponse(){}

    //Getter y setter de Id
    public Long getSaleId(){
        return saleId;
    }

    public void setSaleId(Long saleId){
        this.saleId=saleId;
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

    //Getter y setter changeAmount
    public BigDecimal getChangeAmount(){
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount){
        this.changeAmount=changeAmount;
    }

    //Getter y setter de cashReceived
    public BigDecimal getCashReceived(){
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived){
        this.cashReceived=cashReceived;
    }

    //Getter y setter de paymentStatus
    public PaymentStatus getPaymentStatus(){
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus){
        this.paymentStatus = paymentStatus;
    }

    //Getter y setter de lastFourDigits
    public String getLastFourDigits(){
        return lastFourDigits;
    }

    public void setLastFourDigits(String lasrFourDigits){
        this.lastFourDigits=lasrFourDigits;
    }

    //Getter y setter de authorizationCode
    public String getAuthorizationCode(){
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode){
        this.authorizationCode=authorizationCode;
    }

    //Getter y setter de errorMessage
    public String getErrorMessage(){
        return erroMessage;
    }

    public void setErrorMessage(String errorMessage){
        this.erroMessage=errorMessage;
    }

    //Getter y setter de cardPaymentResponse
    public CardPaymentResponse getCardPaymentResponse(){
        return cardPaymentResponse;
    }

    public void setCardPaymentResponse(CardPaymentResponse cardPaymentResponse){
        this.cardPaymentResponse=cardPaymentResponse;
    }
}

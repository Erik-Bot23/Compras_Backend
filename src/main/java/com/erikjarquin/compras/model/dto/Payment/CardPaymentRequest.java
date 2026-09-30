package com.erikjarquin.compras.model.dto.Payment;

import com.erikjarquin.compras.model.enums.PaymentMethod;

//DTO de pedido de pago con tarjeta
public class CardPaymentRequest {
    private Long saleId; //ID de la venta existente
    private PaymentMethod paymentMethod; //DEBIT o CREDIT
    private String pin;
    private String cardNumber;

    //Getter y setter de saleId
    public Long getSaleId(){
        return saleId;
    }

    public void setSaleId(Long saleId){
        this.saleId=saleId;
    }

    //Getter y setter de paymentMethod
    public PaymentMethod getPaymentMethod(){
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod){
        this.paymentMethod=paymentMethod;
    }

    //Getter y setter de pin
    public String getPin(){
        return pin;
    }

    public void setPin(String pin){
        this.pin=pin;
    }

    //Getter y setter de cardNumber
    public String getCardNumber(){
        return cardNumber;
    }

    public void setCardNumber(String cardNumber){
        this.cardNumber=cardNumber;
    }
}

package com.erikjarquin.compras.model.dto.Sale;

import java.math.BigDecimal;
import java.util.List;

import com.erikjarquin.compras.model.dto.Payment.CardPaymentRequest;
import com.erikjarquin.compras.model.enums.PaymentMethod;

public class SaleRequest {
    private PaymentMethod paymentMethod;
    private BigDecimal cashReceived;
    private List<SaleItemRequest> items;
    private CardPaymentRequest cardPayment; //Pago con tarjeta

    public SaleRequest(){}

    //Getter y setter de paymentMethod
    public PaymentMethod getPaymentMethod(){
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod){
        this.paymentMethod=paymentMethod;
    }

    //Getter y setter de paymentMethod
    public BigDecimal getCashReceived(){
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived){
        this.cashReceived=cashReceived;
    }

    //Getter y setter de paymentMethod
    public List<SaleItemRequest> getItems(){
        return items;
    }

    public void setItems(List<SaleItemRequest> items){
        this.items=items;
    }

    //Getter y setter de cardPayment
    public CardPaymentRequest getCardPayment(){
        return cardPayment;
    }

    public void setCardPayment(CardPaymentRequest cardPayment){
        this.cardPayment=cardPayment;
    }
}

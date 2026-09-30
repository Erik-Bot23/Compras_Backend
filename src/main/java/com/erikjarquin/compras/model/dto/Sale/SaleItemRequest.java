package com.erikjarquin.compras.model.dto.Sale;

public class SaleItemRequest {
    private Long productId;
    private Integer quantity;

    public SaleItemRequest(){}

    //Getter y Setter de id
    public Long getProductId(){
        return productId;
    }

    public void setProductId(Long productId){
        this.productId=productId;
    }

    //Getter y Setter de quantity
    public Integer getQuantity(){
        return quantity;
    }

    public void setQuantity(Integer quantity){
        this.quantity=quantity;
    }
}

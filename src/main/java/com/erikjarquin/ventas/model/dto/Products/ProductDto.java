package com.erikjarquin.ventas.model.dto.Products;

import java.math.BigDecimal;

//DTO de producto
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal cost;
    private int stock;
    private Long categoryId;
    private String categoryName;
    private String img;
    private String sku;
    private String barcode;

    //Constructor vacío
    public ProductDto(){}

    //getters y setters de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //getters y setters de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    //getters y setters de precio
    public BigDecimal getPrice(){
        return price;
    }

    public void setPrice(BigDecimal price){
        this.price=price;
    }

    //getters y setters de stock
    public int getStock(){
        return stock;
    }

    public void setStock(int stock){
        this.stock=stock;
    }

    //getters y setters de cost
    public BigDecimal getCost(){
        return cost;
    }

    public void setCost(BigDecimal cost){
        this.cost=cost;
    }

    //getters y setters de categoria
    public Long getCategoryId(){
        return categoryId;
    }

    public void setCategoryId(Long categoryId){
        this.categoryId=categoryId;
    }

    public String getCategoryName(){
        return categoryName;
    }

    public void setCategoryName(String categoryName){
        this.categoryName=categoryName;
    }

    //getters y setters de imagen
    public String getImg(){
        return img;
    }   

    public void setImg(String img){
        this.img=img;
    }

    //Getter y setter de sku
    public String getSku(){
        return sku;
    }

    public void setSku(String sku){
        this.sku=sku;
    }

    //Getter y setter de barcode
    public String getBarcode(){
        return barcode;
    }

    public void setBarcode(String barcode){
        this.barcode=barcode;
    }
}

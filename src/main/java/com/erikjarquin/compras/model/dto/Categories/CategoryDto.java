package com.erikjarquin.compras.model.dto.Categories;

//DTO de categoría
public class CategoryDto {
    private Long id;
    private String name;

    //Constructor vacío
    public CategoryDto(){}

    //Constructor
    public CategoryDto(Long id, String name){
        this.id=id;
        this.name=name;
    }

    //Getters y setters de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getters y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }
}

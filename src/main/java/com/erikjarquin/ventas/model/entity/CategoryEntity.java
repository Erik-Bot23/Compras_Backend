package com.erikjarquin.ventas.model.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidad {@code categories}: clasifica los productos (la categoría es única).
 * products se serializa como @JsonIgnore para evitar ciclos JSON.
 */
@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @JsonIgnore //Evitar ciclos infinitos al serializar JSON
    @OneToMany(mappedBy = "category")
    private List<ProductEntity> products;

    public CategoryEntity(){}

    public CategoryEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    //getters y setter de id
    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    //getters y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    //getters y setter de List
    public List<ProductEntity> getProducts(){
        return products;
    }

    public void setProducts(List<ProductEntity> products){
        this.products = products;
    }
}

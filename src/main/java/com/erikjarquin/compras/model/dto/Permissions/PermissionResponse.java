package com.erikjarquin.compras.model.dto.Permissions;

//DTO de respuesta de permisos
public class PermissionResponse {
    private Long id;
    private String name;

    //Constructor vacío
    public PermissionResponse(){}

    //Constructor
    public PermissionResponse(Long id, String name){
        this.id=id;
        this.name=name;
    }

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }
}

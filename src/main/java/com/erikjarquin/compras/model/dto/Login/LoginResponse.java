package com.erikjarquin.compras.model.dto.Login;

import java.util.List;

//DTO de respuesta de login
public class LoginResponse {
    private boolean success;
    private Long id;
    private String name;
    private String email;
    private String role;
    private List<String> permissions;
    private String token;

    //Constructor
    public LoginResponse(boolean success, Long id, String name, String email, String role, List<String> permissions, String token){
        this.success=success;
        this.id=id;
        this.name=name;
        this.email=email;
        this.role = role;
        this.permissions=permissions;
        this.token=token;
    }

    //Getter y setter de success
    public boolean isSuccess(){
        return success;
    }

    public void setSuccess(boolean success){
        this.success=success;
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

    //Getter y setter de email
    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email=email;
    }

    //Getter y setter de role
    public String getRole(){
        return role;
    }

    public void setRole(String role){
        this.role=role;
    }

    //Getter y setter de token
    public String getToken(){
        return token;
    }

    public void setToken(String token){
        this.token=token;
    }

    //Getter y setter de permissions
    public List<String> getPermissions(){
        return permissions;
    }

    public void setPermissions(List<String> permissions){
        this.permissions=permissions;
    }
}

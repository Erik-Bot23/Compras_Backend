package com.erikjarquin.compras.model.dto.Login;

//DTO de pedido de login
public class LoginRequest {
    //private String id;
    private String email;
    private String password;

    //getter y setter para email
    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email=email;
    }

    //getter y setter para password
    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password=password;
    }
}

package com.erikjarquin.compras.model.dto.ResetPassword;

//DTO de pedido de contraseña olvidada
public class ForgotPasswordRequest {
    private String email;

    //Getter y settter de email
    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email=email;
    }
}

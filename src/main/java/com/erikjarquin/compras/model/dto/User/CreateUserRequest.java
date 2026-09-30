package com.erikjarquin.compras.model.dto.User;

public class CreateUserRequest {
    private String name;
    private String email;
    private String password;
    private Long roleId;

    //Getter y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    //Getter y setter de email
    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email=email;
    }

    //Getter y setter de password
    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password=password;
    }

    //Getter y setter de role
    public Long getRoleId(){
        return roleId;
    }

    public void setRoleId(Long roleId){
        this.roleId=roleId;
    }
}

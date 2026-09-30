package com.erikjarquin.compras.model.dto.User;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Long roleId;
    private String roleName;
    private boolean active;

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
    public Long getRoleId(){
        return roleId;
    }

    public void setRoleId(Long roleId){
        this.roleId=roleId;
    }

    public String getRoleName(){
        return roleName;
    }

    public void setRoleName(String roleName){
        this.roleName=roleName;
    }

    //Getter y setter de active
     public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        this.active=active;
    }
}

package com.erikjarquin.compras.model.dto.Role;

import java.util.ArrayList;
import java.util.List;

public class UpdateRoleRequest {
    private String name;
    private List<String> permissions = new ArrayList<>();

    public UpdateRoleRequest(){}

    //Getter y setter de name
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    //Getter y setter de permissions
    public List<String> getPermissions(){
        return permissions;
    }

    public void setPermissions(List<String> permissions){
        this.permissions=permissions;
    }
}

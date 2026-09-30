package com.erikjarquin.compras.model.dto.Role;

import java.util.List;

import com.erikjarquin.compras.model.dto.Permissions.PermissionResponse;

public class RoleDto {
    private Long id;
    private String name;
    private List<PermissionResponse> permissions;

    public RoleDto(){}

    public RoleDto(Long id, String name, List<PermissionResponse> permissions){
        this.id=id;
        this.name=name;
        this.permissions=permissions;
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

    //Getter y setter de permissions
    public List<PermissionResponse> getPermissions(){
        return permissions;
    }

    public void setPermissions(List<PermissionResponse> permissions){
        this.permissions=permissions;
    }
}

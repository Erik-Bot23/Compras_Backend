package com.erikjarquin.ventas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Role.CreateRoleRequest;
import com.erikjarquin.ventas.model.dto.Role.RoleDto;
import com.erikjarquin.ventas.model.dto.Role.UpdateRoleRequest;
import com.erikjarquin.ventas.service.RoleService;

/**
 * Roles y su asignación de permisos. Permisos: VER_ROLES, CREAR_ROLES,
 * EDITAR_ROLES, ELIMINAR_ROLES. CORS global (${CORS_ALLOWED_ORIGINS}).
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service){
        this.service=service;
    }

    //Ver todos los roles
    @PreAuthorize("hasAuthority('VER_ROLES')")
    @GetMapping
    public List<RoleDto> getAllRoles(){
        return service.getAllRoles();
    }

    //Ver los roles por su ID
    @PreAuthorize("hasAuthority('VER_ROLES')")
    @GetMapping("/{id}")
    public RoleDto getRoleById(@PathVariable Long id){
        return service.getRoleById(id);
    }

    //Crear role
    @PreAuthorize("hasAuthority('CREAR_ROLES')")
    @PostMapping
    public RoleDto createRole(@RequestBody CreateRoleRequest request){
        return service.createRole(request);
    }

    //Editar role
    @PreAuthorize("hasAuthority('EDITAR_ROLES')")
    @PutMapping("/{id}")
    public RoleDto updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request){
        return service.updateRole(id, request);
    }

    //Eliminar role
    @PreAuthorize("hasAuthority('ELIMINAR_ROLES')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id){
        service.deleteRole(id);
        
        return ResponseEntity.noContent().build();
    }
}

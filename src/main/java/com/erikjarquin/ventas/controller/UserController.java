package com.erikjarquin.ventas.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.User.CreateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UpdateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UserDto;
import com.erikjarquin.ventas.service.UserService;

/**
 * CRUD de usuarios del sistema.
 *
 * <p>Cada acción exige un PERMISO específico vía @PreAuthorize:
 * VER_USUARIOS, CREAR_USUARIOS, EDITAR_USUARIOS, DESACTIVAR_USUARIOS,
 * ACTIVAR_USUARIOS. Sin el token de un usuario con ese permiso → 403.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service){
        this.service = service;
    }

    //Ver todos los usuarios
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    @GetMapping
    public List<UserDto> getAllUsers(){
        return service.getAllUsers();
    }

    //Crear usuario
    @PreAuthorize("hasAuthority('CREAR_USUARIOS')")
    @PostMapping
    public UserDto createUser(@RequestBody CreateUserRequest request){
        return service.createUser(request);
    }

    //Editar usuario
    @PreAuthorize("hasAuthority('EDITAR_USUARIOS')")
    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request){
        return service.updateUser(id, request);
    }

    //Desactivar usuario
    @PreAuthorize("hasAuthority('DESACTIVAR_USUARIOS')")
    @DeleteMapping("/{id}")
    public void deactivateUser(@PathVariable Long id){
        service.deactivateUser(id);
    }

    //Activar usuario
    @PreAuthorize("hasAuthority('ACTIVAR_USUARIOS')")
    @PatchMapping("/{id}/active")
    public void activateUser(@PathVariable Long id){
        service.activateUser(id);
    }

}

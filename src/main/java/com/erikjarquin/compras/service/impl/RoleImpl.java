package com.erikjarquin.compras.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.compras.mapper.RoleMapper;
import com.erikjarquin.compras.model.dto.Role.CreateRoleRequest;
import com.erikjarquin.compras.model.dto.Role.RoleDto;
import com.erikjarquin.compras.model.dto.Role.UpdateRoleRequest;
import com.erikjarquin.compras.model.entity.PermissionEntity;
import com.erikjarquin.compras.model.entity.RoleEntity;
import com.erikjarquin.compras.model.enums.PermissionName;
import com.erikjarquin.compras.repository.PermissionRepository;
import com.erikjarquin.compras.exceptions.RoleException;
import com.erikjarquin.compras.repository.RoleRepository;
import com.erikjarquin.compras.repository.UserRepository;
import com.erikjarquin.compras.service.RoleService;
import com.erikjarquin.compras.util.InputValidator;

/**
 * Implementación de roles y su relación N:M con permisos.
 * Reglas: nombre único y obligatorio, permisos deben existir, y no se puede
 * eliminar un rol que todavía tenga usuarios asignados.
 */
@Service
public class RoleImpl implements RoleService {
    private final RoleRepository repository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleImpl(RoleRepository repository, PermissionRepository permissionRepository, UserRepository userRepository){
        this.repository=repository;
        this.permissionRepository=permissionRepository;
        this.userRepository=userRepository;
    }

    //Mostrar todos los roles
    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles(){
        return repository.findAll().stream().map(RoleMapper::toDto).toList();
    }

    //Buscar role por ID
    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id){
        RoleEntity role = repository.findById(id).orElseThrow(() -> new RoleException("Rol no encontrado"));

        return RoleMapper.toDto(role);
    }

    //Crear role
    @Override
    public RoleDto createRole(CreateRoleRequest request){
        validateRoleName(request.getName());

        if(repository.existsByName(request.getName())){
            throw new RoleException("Ya existe un rol con ese nombre", HttpStatus.CONFLICT);
        }

        Set<PermissionEntity> permissions = findPermissions(request.getPermissions());

        RoleEntity role = new RoleEntity();
        String nombre = InputValidator.texto(request.getName(), "nombre del rol", 50);
        role.setName(nombre);
        role.setPermissions(permissions);

        RoleEntity saved = repository.save(role);

        return RoleMapper.toDto(saved);
    }

    //Actualizar role
    @Override
    public RoleDto updateRole(Long id, UpdateRoleRequest request){
        RoleEntity role = repository.findById(id).orElseThrow(() -> new RoleException("Rol no encontrado"));

        validateRoleName(request.getName());

        if(!role.getName().equals(request.getName()) && repository.existsByName(request.getName())){
            throw new RoleException("Ya existe un rol con ese nombre", HttpStatus.CONFLICT);
        }

        Set<PermissionEntity> permissions = findPermissions(request.getPermissions());
        String nombre = InputValidator.texto(request.getName(), "nombre del rol", 50);
        role.setName(nombre);
        role.setPermissions(permissions);

        RoleEntity updated = repository.save(role);

        return RoleMapper.toDto(updated);
    }

    //Eliminar role
    @Override
    public void deleteRole(Long id){
        repository.findById(id).orElseThrow(() -> 
        new RoleException("Rol no encontrado"));

        //Conteo SQL en vez de la colección lazy users (evita depender de
        //open-in-view y errores de LazyInitialization fuera de transacción).
        if(userRepository.countByRole_Id(id) > 0){
            throw new RoleException("No puedes eliminar un rol con usuarios asignados", HttpStatus.CONFLICT);
        }
        repository.deleteById(id);
    }

    //Encontrar permisos
    private Set<PermissionEntity> findPermissions(List<String> permissionNames){
        //
        if(permissionNames == null || permissionNames.isEmpty()){
            return new HashSet<>();
        }

        List<PermissionName> names = permissionNames.stream().map(this::parsePermission).toList();
        List<PermissionEntity> permissions = permissionRepository.findByNameIn(names);

        if(permissions.size() != names.size()){
            throw new IllegalArgumentException("Uno o más permisos no existen");
        }

        return new HashSet<>(permissions);
    }

    //Revisar que un permiso sea válido
    private PermissionName parsePermission(String permission){
        try {
            return PermissionName.valueOf(permission);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Permiso inválido: " + permission);
        }
    }

    //Validar que se le asigne nombre al rol
    private void validateRoleName(String name){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
    }

}


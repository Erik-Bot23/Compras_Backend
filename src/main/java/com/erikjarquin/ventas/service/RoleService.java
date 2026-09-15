package com.erikjarquin.ventas.service;

import java.util.List;

import com.erikjarquin.ventas.model.dto.Role.CreateRoleRequest;
import com.erikjarquin.ventas.model.dto.Role.RoleDto;
import com.erikjarquin.ventas.model.dto.Role.UpdateRoleRequest;

/**
 * Contrato de operaciones sobre roles y su asignación de permisos.
 * Ver implementación en {@code service/impl/RoleImpl}.
 */
public interface RoleService {
    List<RoleDto> getAllRoles();
    RoleDto getRoleById(Long id);
    RoleDto createRole(CreateRoleRequest request);
    RoleDto updateRole(Long id, UpdateRoleRequest request);
    void deleteRole(Long id);
}

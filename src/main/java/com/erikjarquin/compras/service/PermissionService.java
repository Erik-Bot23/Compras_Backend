package com.erikjarquin.compras.service;

import java.util.List;

import com.erikjarquin.compras.model.dto.Permissions.PermissionResponse;

/**
 * Contrato de permisos disponibles del sistema.
 * Ver {@code service/impl/PermissionImpl}.
 */
public interface PermissionService {
    List<PermissionResponse> getAllPermissions();
}

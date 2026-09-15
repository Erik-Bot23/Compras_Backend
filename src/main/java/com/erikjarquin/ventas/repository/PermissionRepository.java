package com.erikjarquin.ventas.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erikjarquin.ventas.model.entity.PermissionEntity;
import com.erikjarquin.ventas.model.enums.PermissionName;

/**
 * Repositorio de permisos. findByNameIn es usado por PermissionBootstrap y
 * RolePermissionBootstrap para asignar permisos a roles de una sola vez.
 */
@Repository //
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long>{
    //Buscar permiso
    Optional<PermissionEntity> findByName(PermissionName name);
    
    //Buscar permisos
    List<PermissionEntity> findByNameIn(Collection<PermissionName> names);
}

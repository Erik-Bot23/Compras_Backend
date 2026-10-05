package com.erikjarquin.compras.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.erikjarquin.compras.model.entity.PermissionEntity;
import com.erikjarquin.compras.model.enums.PermissionName;

/**
 * Repositorio de permisos. findByNameIn es usado por PermissionBootstrap y
 * RolePermissionBootstrap para asignar permisos a roles de una sola vez.
 */
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long>{
    //Buscar permiso
    Optional<PermissionEntity> findByName(PermissionName name);
    
    //Buscar permisos
    List<PermissionEntity> findByNameIn(Collection<PermissionName> names);
}

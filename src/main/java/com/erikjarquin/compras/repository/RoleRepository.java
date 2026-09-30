package com.erikjarquin.compras.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.compras.model.entity.RoleEntity;

/**
 * Repositorio de roles. findByName es usado por los bootstraps (búsqueda por
 * nombre de rol) y existsByName para evitar duplicar roles.
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    //Encontrar role por nombre
    Optional<RoleEntity> findByName(String name);
    
    //Revisar existencia
    boolean existsByName(String name);
}

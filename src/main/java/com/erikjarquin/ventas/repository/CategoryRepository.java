package com.erikjarquin.ventas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.ventas.model.entity.CategoryEntity;

/**
 * Repositorio de categorías. findByName es usado para validar que no se
 * creen dos categorías con el mismo nombre (nombre es UNIQUE en BD).
 */
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByName(String name);
}

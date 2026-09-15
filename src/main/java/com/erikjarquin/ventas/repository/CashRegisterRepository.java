package com.erikjarquin.ventas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.ventas.model.entity.CashRegisterEntity;

/**
 * Repositorio de cajas. findByActiveTrue devuelve la caja vigente; la
 * existencia de UNA caja abierta es la invariante del módulo (la regla la
 * aplica CashRegisterImpl, no la BD).
 */
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, Long> {
    Optional<CashRegisterEntity> findByActiveTrue();
}

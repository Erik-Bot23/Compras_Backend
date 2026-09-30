package com.erikjarquin.compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.compras.model.entity.CashRegisterEntity;

/**
 * Repositorio de cajas. findByActiveTrue devuelve la caja vigente; la
 * existencia de UNA caja abierta es la invariante del módulo (la regla la
 * aplica CashRegisterImpl, no la BD).
 */
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, Long> {
    Optional<CashRegisterEntity> findByActiveTrue();

    //V3: el numero de caja. existsByNumber se usa al ABRIR para dar un 409 con
    //mensaje en vez de un 500 por violar el UNIQUE de la BD.
    boolean existsByNumber(String number);

    Optional<CashRegisterEntity> findByNumber(String number);

    //Historial para el filtro por caja de Reportes (la mas reciente primero)
    List<CashRegisterEntity> findAllByOrderByOpenedAtDesc();
}

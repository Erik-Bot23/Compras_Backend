package com.erikjarquin.ventas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.ventas.model.entity.PurchaseEntity;

/**
 * Repositorio de compras (cabecera).
 *
 * <p>findAll/findByProvider/findDetailedById usan {@code @EntityGraph} para
 * precargar provider + details.product en UNA consulta (evita N+1 en los
 * listados) y así el mapper puede leer product.name sin volver a tocar la BD.
 */
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {

    @EntityGraph(attributePaths = {"provider", "details.product"})
    List<PurchaseEntity> findAllByOrderByPurchaseDateDesc();

    //Compras de un proveedor concreto (listado por proveedor)
    @EntityGraph(attributePaths = {"provider", "details.product"})
    List<PurchaseEntity> findByProviderIdOrderByPurchaseDateDesc(Long providerId);

    //Compra completa (GET /{id}: detalles + producto + proveedor)
    @EntityGraph(attributePaths = {"provider", "details.product"})
    Optional<PurchaseEntity> findDetailedById(Long id);

    //¿Cuántas compras tiene un proveedor? (para bloquear su borrado con 409)
    long countByProvider_Id(Long providerId);
}
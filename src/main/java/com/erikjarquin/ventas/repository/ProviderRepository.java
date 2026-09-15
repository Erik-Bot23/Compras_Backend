package com.erikjarquin.ventas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.ventas.model.entity.ProviderEntity;

/**
 * Repositorio de proveedores. rfc es UNIQUE en BD; findByRfc sirve para
 * detectar duplicados con mensaje amigable (409) antes del constraint.
 */
public interface ProviderRepository extends JpaRepository<ProviderEntity, Long> {
    //Buscar por RFC (control de duplicados)
    Optional<ProviderEntity> findByRfc(String rfc);

    //Listado alfabético para selects del frontend
    List<ProviderEntity> findAllByOrderByNameAsc();
}
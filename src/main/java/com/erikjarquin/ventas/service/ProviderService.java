package com.erikjarquin.ventas.service;

import java.util.List;

import com.erikjarquin.ventas.model.dto.Purchases.ProviderDto;

/**
 * Contrato del módulo de PROVEEDORES (CRUD atrás de /api/providers).
 * Ver {@code service/impl/ProviderImpl}.
 */
public interface ProviderService {
    //Listado alfabético (para selects y tablas)
    List<ProviderDto> getAll();

    //Buscar por id
    ProviderDto getById(Long id);

    //Crear proveedor (valida nombre/RFC obligatorios y RFC único)
    ProviderDto save(ProviderDto dto);

    //Editar proveedor
    ProviderDto update(Long id, ProviderDto dto);

    //Borrar proveedor (409 si tiene compras registradas)
    void delete(Long id);
}
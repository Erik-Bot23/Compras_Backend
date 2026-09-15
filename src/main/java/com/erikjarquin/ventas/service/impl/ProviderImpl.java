package com.erikjarquin.ventas.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.exceptions.ProviderException;
import com.erikjarquin.ventas.mapper.ProviderMapper;
import com.erikjarquin.ventas.model.dto.Purchases.ProviderDto;
import com.erikjarquin.ventas.model.entity.ProviderEntity;
import com.erikjarquin.ventas.repository.ProviderRepository;
import com.erikjarquin.ventas.repository.PurchaseRepository;
import com.erikjarquin.ventas.service.ProviderService;

/**
 * Implementación del CRUD de proveedores.
 *
 * <p>Reglas de negocio: nombre y RFC son obligatorios (400); RFC único (409);
 * no se puede borrar un proveedor que tenga compras (409, integridad del
 * histórico). No encontrado → 404 vía ProviderException por defecto.
 */
@Service
public class ProviderImpl implements ProviderService {
    private final ProviderRepository repository;
    private final PurchaseRepository purchaseRepository;

    public ProviderImpl(ProviderRepository repository, PurchaseRepository purchaseRepository){
        this.repository = repository;
        this.purchaseRepository = purchaseRepository;
    }

    //Listar proveedores (orden alfabético para los selects del frontend)
    @Override
    public List<ProviderDto> getAll(){
        return repository.findAllByOrderByNameAsc()
                .stream()
                .map(ProviderMapper::toDto)
                .collect(Collectors.toList());
    }

    //Buscar proveedor por id
    @Override
    public ProviderDto getById(Long id){
        return ProviderMapper.toDto(findOrThrow(id));
    }

    //Crear proveedor
    @Override
    public ProviderDto save(ProviderDto dto){
        validateRequired(dto);

        if(repository.findByRfc(dto.getRfc()).isPresent()){
            throw new ProviderException("Ya existe un proveedor con ese RFC", HttpStatus.CONFLICT);
        }

        ProviderEntity saved = repository.save(ProviderMapper.toEntity(dto));
        return ProviderMapper.toDto(saved);
    }

    //Editar proveedor
    @Override
    public ProviderDto update(Long id, ProviderDto dto){
        ProviderEntity entity = findOrThrow(id);
        validateRequired(dto);

        repository.findByRfc(dto.getRfc()).ifPresent(existing -> {
            if(!existing.getId().equals(id)){
                throw new ProviderException("Ya existe un proveedor con ese RFC", HttpStatus.CONFLICT);
            }
        });

        entity.setName(dto.getName());
        entity.setRfc(dto.getRfc());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());

        return ProviderMapper.toDto(repository.save(entity));
    }

    //Borrar proveedor (solo si no tiene compras)
    @Override
    public void delete(Long id){
        findOrThrow(id); //404 si no existe

        if(purchaseRepository.countByProvider_Id(id) > 0){
            throw new ProviderException(
                "No puedes eliminar un proveedor con compras registradas",
                HttpStatus.CONFLICT);
        }

        repository.deleteById(id);
    }

    //Buscar proveedor o lanzar 404
    private ProviderEntity findOrThrow(Long id){
        return repository.findById(id).orElseThrow(() ->
            new ProviderException("Proveedor no encontrado"));
    }

    //Validar campos obligatorios (400)
    private void validateRequired(ProviderDto dto){
        if(dto.getName() == null || dto.getName().trim().isEmpty()){
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        if(dto.getRfc() == null || dto.getRfc().trim().isEmpty()){
            throw new IllegalArgumentException("El RFC del proveedor es obligatorio");
        }
    }
}
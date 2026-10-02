package com.erikjarquin.compras.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erikjarquin.compras.exceptions.ProviderException;
import com.erikjarquin.compras.mapper.ProviderMapper;
import com.erikjarquin.compras.util.InputValidator;
import com.erikjarquin.compras.model.dto.Purchases.ProviderDto;
import com.erikjarquin.compras.model.entity.ProviderEntity;
import com.erikjarquin.compras.repository.ProviderRepository;
import com.erikjarquin.compras.repository.PurchaseRepository;
import com.erikjarquin.compras.service.ProviderService;

/**
 * Implementación del CRUD de proveedores.
 *
 * Reglas de negocio: nombre y RFC son obligatorios (400); RFC único (409);
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

        //Normalizamos antes de mapear
        dto.setPhone(InputValidator.telefono(dto.getPhone()));
        dto.setEmail(InputValidator.email(dto.getEmail()));

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

        entity.setName(normalizarNombre(dto.getName()));
        entity.setPhone(InputValidator.telefono(dto.getPhone()));
        entity.setEmail(InputValidator.email(dto.getEmail()));
        entity.setRfc(InputValidator.rfc(dto.getRfc()));
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

    /**
     * Normaliza el nombre del proveedor: recorta y pasa a MAYÚSCULAS.
     *
     * El nombre del proveedor sí va en mayúsculas (a diferencia del nombre de
     * los productos): es un dato corto, casi siempre una razón social, y en
     * mayúsculas se ve uniforme en las tablas y evita que "Dairy Queen" y
     * "DAIRY QUEEN" se guarden como dos proveedores distintos.
     *
     * Recortar es lo importante: los espacios al final invisiblemente rompen
     * el UNIQUE del RFC cuando se busca por duplicado.
     */
    private String normalizarNombre(String nombre){
        return nombre == null ? null : nombre.trim().toUpperCase();
    }
}
package com.erikjarquin.ventas.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.exceptions.CategoryException;
import com.erikjarquin.ventas.mapper.CategoryMapper;
import com.erikjarquin.ventas.model.dto.Categories.CategoryDto;
import com.erikjarquin.ventas.model.entity.CategoryEntity;
import com.erikjarquin.ventas.repository.CategoryRepository;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.service.CategoryService;

/**
 * Implementación de categorías de productos. No deja borrar una categoría
 * que tenga productos asociados (integrridad referencial) ni guardar dos
 * categorías con el mismo nombre (name es UNIQUE en BD).
 */
@Service
public class CategoryImpl implements CategoryService {
    private final CategoryRepository repository;
    private final ProductRepository productRepository;

    public CategoryImpl(CategoryRepository repository, ProductRepository productRepository){
        this.repository=repository;
        this.productRepository=productRepository;
    }

    //Mostrar todas las categorías
    @Override
    public List<CategoryDto> getAll(){
        return repository.findAll()
                .stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList()); //collect: 
    }

    //Guaradar la nueva categoría
    @Override
    public CategoryDto save(CategoryDto dto){
        validateCategoryName(dto.getName());

        if(repository.findByName(dto.getName()).isPresent()){
            throw new CategoryException("Ya existe una categoría con ese nombre", HttpStatus.CONFLICT);
        }

        CategoryEntity entity = CategoryMapper.toEntity(dto);
        CategoryEntity saved = repository.save(entity);

        return CategoryMapper.toDto(saved);
    }

    //Editar una categoría
    @Override
    public CategoryDto update(Long id, CategoryDto dto){
        CategoryEntity category = repository.findById(id).orElseThrow(() ->
            new CategoryException("Categoría no encontrada"));

        validateCategoryName(dto.getName());

        repository.findByName(dto.getName()).ifPresent(existing -> {
            if(!existing.getId().equals(id)){
                throw new CategoryException("Ya existe una categoría con ese nombre", HttpStatus.CONFLICT);
            }
        });

        category.setName(dto.getName());
        CategoryEntity updated = repository.save(category);

        return CategoryMapper.toDto(updated);
    }

    //Borrar la categoría
    @Override
    public void delete(Long id){
        repository.findById(id).orElseThrow(() ->
            new CategoryException("Categoría no encontrada"));

        //Conteo SQL en vez de la colección lazy products (evita depender de
        //open-in-view y errores de LazyInitialization fuera de transacción).
        if(productRepository.countByCategory_Id(id) > 0){
            throw new CategoryException("No puedes eliminar una categoría con productos", HttpStatus.CONFLICT);
        }

        repository.deleteById(id);
    }

    //Validar que la categoría lleve nombre
    private void validateCategoryName(String name){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
    }
}

package com.erikjarquin.compras.service;

import java.util.List;

import com.erikjarquin.compras.model.dto.Categories.CategoryDto;

/**
 * Contrato de categorías de productos (listar, crear, editar, eliminar).
 */
public interface CategoryService {
    List<CategoryDto> getAll();
    CategoryDto save(CategoryDto dto);
    CategoryDto update(Long id, CategoryDto dto);
    void delete(Long id);
}

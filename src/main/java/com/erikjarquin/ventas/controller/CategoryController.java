package com.erikjarquin.ventas.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Categories.CategoryDto;
import com.erikjarquin.ventas.service.CategoryService;

/**
 * Categorías de productos: listar, crear, editar y eliminar.
 * Permisos: VER_CATEGORIAS, CREAR_CATEGORIAS, EDITAR_CATEGORIAS,
 * ELIMINAR_CATEGORIAS.
 * CORS global (${CORS_ALLOWED_ORIGINS}) en SecurityConfig.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service){
        this.service=service;
    }

    //Ver todas las categorías
    @PreAuthorize("hasAuthority('VER_CATEGORIAS')")
    @GetMapping
    public List<CategoryDto> getAll(){
        return service.getAll();
    }

    //Guardar una nueva categoría
    @PreAuthorize("hasAuthority('CREAR_CATEGORIAS')")
    @PostMapping
    public CategoryDto save(@RequestBody CategoryDto dto){
        return service.save(dto);
    }

    //Editar una categoría
    @PreAuthorize("hasAuthority('EDITAR_CATEGORIAS')")
    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable Long id, @RequestBody CategoryDto dto){
        return service.update(id, dto);
    }

    //Borrar categoría
    @PreAuthorize("hasAuthority('ELIMINAR_CATEGORIAS')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}

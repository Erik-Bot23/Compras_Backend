package com.erikjarquin.ventas.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.erikjarquin.ventas.model.dto.Products.ProductDto;

/**
 * Contrato del catálogo de productos (CRUD, filtro, búsquedas).
 * Las operaciones de alta/edición reciben la imagen como MultipartFile y la
 * guardan mediante FileStorageService. Ver {@code service/impl/ProductImpl}.
 */
public interface ProductService {
    //Listar todos los productos
    List<ProductDto> getAll();
    //Filtrar por categoría
    List<ProductDto> getByCategory(String category); //¿Long categoryId?
    
    //Guardar producto
    ProductDto save(
        String name,
        BigDecimal price,
        int stock,
        Long categoryId,
        String sku,
        String barcode,
        MultipartFile image
    );
    //Actualizar producto
    ProductDto update(
        Long id,
        String name,
        BigDecimal price,
        int stock,
        Long categoryId,
        String sku,
        String barcode,
        MultipartFile image
    );

    void delete(Long id);
    //Buscar por código de barras
    ProductDto findByBarcode(String barcode);
    //Acción de buscador
    List<ProductDto> search(String q);
} 

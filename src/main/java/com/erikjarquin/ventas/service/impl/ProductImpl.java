package com.erikjarquin.ventas.service.impl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.erikjarquin.ventas.exceptions.ProductException;
import com.erikjarquin.ventas.mapper.ProductMapper;
import com.erikjarquin.ventas.model.dto.Products.ProductDto;
import com.erikjarquin.ventas.model.entity.CategoryEntity;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.repository.CategoryRepository;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.service.FileStorageService;
import com.erikjarquin.ventas.service.ProductService;

/**
 * Implementación del catálogo de productos.
 *
 * <p>La imagen se guarda con FileStorageService (que valida tamaño/extensión/
 * contenido y devuelve el nombre UUID). El mapper construye la URL completa:
 * {@code app.upload-url}/{archivo} para que el frontend la use en <img>.
 *
 * <p>Los errores de "no encontrado" o categoría inválida se lanzan como
 * IllegalArgumentException → el GlobalExceptionHandler responde 400 (no 500).
 */
@Service
public class ProductImpl implements ProductService {
    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ProductMapper productMapper;
    
    public ProductImpl(ProductRepository repository, CategoryRepository categoryRepository, FileStorageService fileStorageService, ProductMapper productMapper){
        this.repository=repository;
        this.categoryRepository=categoryRepository;
        this.fileStorageService = fileStorageService;
        this.productMapper = productMapper;
    }

    //Listar todos los productos ACTIVOS (los dados de baja van a getInactive)
    @Override
    public List<ProductDto> getAll(){
        Set<Long> withHistory = productIdsWithHistory();
        return repository.findByActiveTrue().stream()
                .map(product -> productMapper.toDto(product, withHistory.contains(product.getId())))
                .collect(Collectors.toList());
    }

    //Listar productos por categoría (excluye los dados de baja)
    @Override
    public List<ProductDto> getByCategory(String category){
        Set<Long> withHistory = productIdsWithHistory();
        return repository.findByCategory_NameAndActiveTrue(category).stream()
                .map(product -> productMapper.toDto(product, withHistory.contains(product.getId())))
                .collect(Collectors.toList());
    }

    //Listar SOLO los productos dados de baja (borrado lógico)
    @Override
    public List<ProductDto> getInactive(){
        Set<Long> withHistory = productIdsWithHistory();
        return repository.findByActiveFalse().stream()
                .map(product -> productMapper.toDto(product, withHistory.contains(product.getId())))
                .collect(Collectors.toList());
    }

    //Guardar nuevo producto
    @Override
    public ProductDto save(
        String name,
        BigDecimal price,
        int stock,
        Long categoryId,
        String sku,
        String barcode,
        MultipartFile image
    ){
        ProductEntity entity = new ProductEntity();

        entity.setName(name);
        entity.setPrice(price);
        entity.setStock(stock);

        CategoryEntity category = categoryRepository.findById(categoryId).orElseThrow(() -> 
                        new IllegalArgumentException("Categoria no encontrada"));
                                        
        entity.setCategory(category);
        entity.setSku(sku);
        entity.setBarcode(barcode);

        entity.setImg(fileStorageService.store(image));

        ProductEntity saved = repository.save(entity);

        return productMapper.toDto(saved);
    }

    //Actualizar producto
    @Override
    public ProductDto update(
        Long id,
        String name,
        BigDecimal price,
        int stock,
        Long categoryId,
        String sku,
        String barcode,
        MultipartFile image
    ){
        ProductEntity entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        entity.setName(name);
        entity.setPrice(price);
        entity.setStock(stock);

        CategoryEntity category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        entity.setCategory(category);
        entity.setSku(sku);
        entity.setBarcode(barcode);

        if(image != null && !image.isEmpty()){
            fileStorageService.delete(entity.getImg());
            entity.setImg(fileStorageService.store(image));
        }

        ProductEntity updated = repository.save(entity);

        return productMapper.toDto(updated);
    }

    //Borrar producto
    @Override
    public void delete(Long id){
        ProductEntity entity = repository.findById(id).orElseThrow(() -> new ProductException("Producto no encontrado"));

        //No se puede borrar un producto con histórico de ventas o compras:
        //la venta/compra lo referencia por FK y perderíamos el dato histórico.
        if(hasHistory(id)){
            throw new ProductException("No se puede eliminar el producto: tiene ventas o compras asociadas. Puedes darlo de baja para quitarlo del catálogo sin perder el histórico", HttpStatus.CONFLICT);
        }

        fileStorageService.delete(entity.getImg());
        repository.deleteById(id);
    }

    //Dar de baja un producto (borrado lógico): no borra la fila ni la imagen
    @Override
    public ProductDto deactivate(Long id){
        ProductEntity entity = repository.findById(id).orElseThrow(() ->
            new ProductException("Producto no encontrado"));

        //Evita repetir la operación: ya está dado de baja.
        if(!entity.isActive()){
            throw new ProductException("El producto ya está dado de baja", HttpStatus.CONFLICT);
        }

        entity.setActive(false);
        ProductEntity saved = repository.save(entity);

        return productMapper.toDto(saved, hasHistory(id));
    }

    //Reactivar un producto dado de baja (active=true)
    @Override
    public ProductDto activate(Long id){
        ProductEntity entity = repository.findById(id).orElseThrow(() ->
            new ProductException("Producto no encontrado"));

        //Evita repetir la operación: ya está activo.
        if(entity.isActive()){
            throw new ProductException("El producto ya está activo", HttpStatus.CONFLICT);
        }

        entity.setActive(true);
        ProductEntity saved = repository.save(entity);

        return productMapper.toDto(saved, hasHistory(id));
    }

    //Buscar productos por código de barras
    @Override
    public ProductDto findByBarcode(String barcode){
        ProductEntity product = repository.findByBarcode(barcode).orElseThrow(() ->
            new IllegalArgumentException("Producto no encontrado"));

        //Un producto dado de baja se trata como inexistente para el POS (no se vende).
        if(!product.isActive()){
            throw new IllegalArgumentException("Producto no encontrado");
        }

        return productMapper.toDto(product);
    }

    //Buscador de productos (el repositorio ya excluye los dados de baja)
    @Override
    public List<ProductDto> search(String q){
        return repository.search(q).stream().map(productMapper::toDto).collect(Collectors.toList());
    }

    //¿El producto tiene ventas o compras registradas? (bloquea el borrado real)
    private boolean hasHistory(Long id){
        return repository.existsBySaleDetailsProductId(id) || repository.existsByPurchaseDetailsProductId(id);
    }

    //Set de ids con ventas/compras, calculado en 2 consultas (evita N+1 al listar)
    private Set<Long> productIdsWithHistory(){
        Set<Long> ids = new HashSet<>(repository.findProductIdsWithSales());
        ids.addAll(repository.findProductIdsWithPurchases());
        return ids;
    }

}

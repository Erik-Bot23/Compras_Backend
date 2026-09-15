package com.erikjarquin.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erikjarquin.ventas.model.dto.Products.ProductDto;
import com.erikjarquin.ventas.service.ProductService;

/**
 * CRUD de productos (multipart para la imagen) y búsquedas.
 *
 * <p>Permisos requeridos: VER_PRODUCTOS (listar/buscar), CREAR_PRODUCTOS,
 * EDITAR_PRODUCTOS, ELIMINAR_PRODUCTOS.
 *
 * <p>Nota CORS: el origen permitido se define globalmente en SecurityConfig
 * (propiedad ${CORS_ALLOWED_ORIGINS}); por eso aquí ya NO hay @CrossOrigin.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service){
        this.service = service;
    }

    //Ver productos
    @PreAuthorize("hasAuthority('VER_PRODUCTOS')")
    @GetMapping
    public List<ProductDto> getProducts(@RequestParam(required = false) String category){ 
        if (category != null) {
            return service.getByCategory(category);
        }
        return service.getAll();
    }
    
    //Crear un nuevo producto
    @PreAuthorize("hasAuthority('CREAR_PRODUCTOS')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDto saveProduct(
        @RequestParam("name") String name,
        @RequestParam("price") BigDecimal price,
        @RequestParam("stock") int stock,
        @RequestParam("categoryId") Long categoryId,
        @RequestParam("sku") String sku,
        @RequestParam("barcode") String barcode,
        @RequestParam(value = "image", required = false) MultipartFile image
    ){
        return service.save(name, price, stock, categoryId, sku, barcode, image); 
    }

    //Editar un producto
    @PreAuthorize("hasAuthority('EDITAR_PRODUCTOS')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDto updateProduct(
        @PathVariable Long id,
        @RequestParam String name,
        @RequestParam BigDecimal price,
        @RequestParam int stock,
        @RequestParam Long categoryId,
        @RequestParam String sku,
        @RequestParam String barcode,
        @RequestParam(value = "image", required = false) MultipartFile image
    ){
        return service.update(id, name, price, stock, categoryId, sku, barcode, image);
    }

    //Eliminar producto con el ID
    @PreAuthorize("hasAuthority('ELIMINAR_PRODUCTOS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    //Buscar productos con el código de barras
    @PreAuthorize("hasAuthority('VER_PRODUCTOS')")
    @GetMapping("/barcode/{barcode}")
    public ProductDto findByBarcode(@PathVariable String barcode){
        return service.findByBarcode(barcode);
    }

    //Buscar productos por nombre
    @PreAuthorize("hasAuthority('VER_PRODUCTOS')")
    @GetMapping("/search")
    public List<ProductDto> search(@RequestParam String q){
        return service.search(q);
    }
}

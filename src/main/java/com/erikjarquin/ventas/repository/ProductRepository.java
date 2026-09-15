package com.erikjarquin.ventas.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erikjarquin.ventas.model.entity.ProductEntity;

/**
 * Repositorio de productos: consultas por categoría, código de barras y el
 * buscador libre (búsqueda insensible a mayúsculas sobre nombre/sku/barcode,
 * usada por GET /api/products/search?q=).
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    //Buscar producto por categoria
    List<ProductEntity> findByCategory_Name(String name);

    //Buscar producto por código de barras
    Optional<ProductEntity> findByBarcode(String barcode);

    @Query("""
        SELECT p
        FROM ProductEntity p
        WHERE
        LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :q, '%'))
    """)

    //Buscar producto con el buscador
    List<ProductEntity> search(@Param("q") String q);

    //Productos con stock bajo o agotado (reporte de inventario)
    @Query("""
        SELECT p
        FROM ProductEntity p
        WHERE p.stock <= :threshold
        ORDER BY p.stock ASC
    """)
    List<ProductEntity> findLowStock(@Param("threshold") int threshold);

    //¿El producto se ha vendido alguna vez? (para bloquear su borrado con 409)
    @Query("SELECT COUNT(d) > 0 FROM SaleDetailEntity d WHERE d.product.id = :productId")
    boolean existsBySaleDetailsProductId(@Param("productId") Long productId);

    //¿El producto se ha comprado alguna vez? (para bloquear su borrado con 409)
    @Query("SELECT COUNT(d) > 0 FROM PurchaseDetailEntity d WHERE d.product.id = :productId")
    boolean existsByPurchaseDetailsProductId(@Param("productId") Long productId);

    //¿Cuántos productos tiene una categoría? (para bloquear su borrado con 409)
    long countByCategory_Id(Long categoryId);
}
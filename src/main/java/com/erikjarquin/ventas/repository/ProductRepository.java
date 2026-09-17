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
    //Productos ACTIVOS (no dados de baja). Es el listado normal del catálogo.
    List<ProductEntity> findByActiveTrue();

    //Productos DADOS DE BAJA (borrado lógico). Alimenta la vista "Dados de baja".
    List<ProductEntity> findByActiveFalse();

    //Buscar producto por categoria (sin filtrar estado; se conserva por compatibilidad)
    List<ProductEntity> findByCategory_Name(String name);

    //Productos activos de una categoría (filtra los dados de baja)
    List<ProductEntity> findByCategory_NameAndActiveTrue(String name);

    //Buscar producto por código de barras
    Optional<ProductEntity> findByBarcode(String barcode);

    @Query("""
        SELECT p
        FROM ProductEntity p
        WHERE p.active = true
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :q, '%'))
        )
    """)

    //Buscar producto con el buscador (excluye los dados de baja)
    List<ProductEntity> search(@Param("q") String q);

    //Productos con stock bajo o agotado (reporte de inventario; solo activos)
    @Query("""
        SELECT p
        FROM ProductEntity p
        WHERE p.stock <= :threshold
        AND p.active = true
        ORDER BY p.stock ASC
    """)
    List<ProductEntity> findLowStock(@Param("threshold") int threshold);

    //IDs de productos que YA tienen ventas. Se usa para calcular hasHistory en
    //una sola consulta (y no con un exists por producto, que sería N+1).
    @Query("SELECT DISTINCT d.product.id FROM SaleDetailEntity d WHERE d.product IS NOT NULL")
    List<Long> findProductIdsWithSales();

    //IDs de productos que YA tienen compras (mismo propósito que el anterior).
    @Query("SELECT DISTINCT d.product.id FROM PurchaseDetailEntity d WHERE d.product IS NOT NULL")
    List<Long> findProductIdsWithPurchases();

    //¿El producto se ha vendido alguna vez? (para bloquear su borrado con 409)
    @Query("SELECT COUNT(d) > 0 FROM SaleDetailEntity d WHERE d.product.id = :productId")
    boolean existsBySaleDetailsProductId(@Param("productId") Long productId);

    //¿El producto se ha comprado alguna vez? (para bloquear su borrado con 409)
    @Query("SELECT COUNT(d) > 0 FROM PurchaseDetailEntity d WHERE d.product.id = :productId")
    boolean existsByPurchaseDetailsProductId(@Param("productId") Long productId);

    //¿Cuántos productos tiene una categoría? (para bloquear su borrado con 409)
    long countByCategory_Id(Long categoryId);
}
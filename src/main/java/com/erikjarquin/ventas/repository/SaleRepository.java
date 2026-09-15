package com.erikjarquin.ventas.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erikjarquin.ventas.model.entity.CashRegisterEntity;
import com.erikjarquin.ventas.model.entity.SaleEntity;
import com.erikjarquin.ventas.model.enums.PaymentStatus;

/**
 * Repositorio de ventas. Además del corte de caja
 * ({@code findByCashRegister}) contiene las consultas de AGRAGADOS del módulo
 * de reportes ({@code ReportsImpl}), todas acotadas por rango de fechas y
 * estado APPROVED para no contabilizar ventas canceladas/reversadas.
 */
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    List<SaleEntity> findByCashRegister(CashRegisterEntity cashRegister);

    //Tendencia por día
    @Query("""
        SELECT CAST(s.saleDate AS date), SUM(s.total), COUNT(s)
        FROM SaleEntity s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY CAST(s.saleDate AS date)
        ORDER BY CAST(s.saleDate AS date)
    """)
    List<Object[]> sumSalesByDay(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);

    //Tendencia por mes
    @Query("""
        SELECT year(CAST(s.saleDate AS date)), month(CAST(s.saleDate AS date)), SUM(s.total), COUNT(s)
        FROM SaleEntity s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY year(CAST(s.saleDate AS date)), month(CAST(s.saleDate AS date))
        ORDER BY year(CAST(s.saleDate AS date)), month(CAST(s.saleDate AS date))
    """)
    List<Object[]> sumSalesByMonth(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);

    //Tendencia por año
    @Query("""
        SELECT year(CAST(s.saleDate AS date)), SUM(s.total), COUNT(s)
        FROM SaleEntity s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY year(CAST(s.saleDate AS date))
        ORDER BY year(CAST(s.saleDate AS date))
    """)
    List<Object[]> sumSalesByYear(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);

    //Productos más vendidos (por unidades)
    @Query("""
        SELECT d.product.id, d.product.name, SUM(d.quantity), SUM(d.subtotal)
        FROM SaleDetailEntity d JOIN d.sale s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY d.product.id, d.product.name
        ORDER BY SUM(d.quantity) DESC
    """)
    List<Object[]> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status,
            Pageable pageable);

    //Distribución por método de pago
    @Query("""
        SELECT s.paymentMethod, SUM(s.total), COUNT(s)
        FROM SaleEntity s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY s.paymentMethod
        ORDER BY s.paymentMethod
    """)
    List<Object[]> findPaymentMethodDistribution(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);

    //Rendimiento por categoría
    @Query("""
        SELECT c.id, c.name, SUM(d.quantity), SUM(d.subtotal)
        FROM SaleDetailEntity d JOIN d.sale s LEFT JOIN d.product p LEFT JOIN p.category c
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
        GROUP BY c.id, c.name
        ORDER BY SUM(d.subtotal) DESC
    """)
    List<Object[]> findCategoryPerformance(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);

    //Totales del rango (conteo, suma y ticket promedio)
    @Query("""
        SELECT COUNT(s), SUM(s.total), AVG(s.total)
        FROM SaleEntity s
        WHERE s.paymentStatus = :status AND s.saleDate >= :from AND s.saleDate < :to
    """)
    List<Object[]> sumSalesTotals(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") PaymentStatus status);
}
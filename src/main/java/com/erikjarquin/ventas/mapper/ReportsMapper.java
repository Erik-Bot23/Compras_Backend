package com.erikjarquin.ventas.mapper;

import java.math.BigDecimal;

import com.erikjarquin.ventas.model.dto.Reports.CategoryPerformanceDTO;
import com.erikjarquin.ventas.model.dto.Reports.LowStockDTO;
import com.erikjarquin.ventas.model.dto.Reports.MarginDTO;
import com.erikjarquin.ventas.model.dto.Reports.PaymentMethodDTO;
import com.erikjarquin.ventas.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.ventas.model.dto.Reports.TopProductDTO;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.enums.PaymentMethod;

/**
 * Mapper estático de los agregados del módulo de reportes ({@code Reports}).
 * Recibe los valores de las consultas JPQL (Object/Number ya desempaquetados)
 * y devuelve los DTO que consume el dashboard del frontend.
 */
public class ReportsMapper {

    private ReportsMapper(){}

    //Tendencia de ventas (un punto por periodo)
    public static PeriodSalesDTO toPeriodSalesDTO(String period, BigDecimal total, Long count){
        return new PeriodSalesDTO(period, total, count);
    }

    //Producto más vendido
    public static TopProductDTO toTopProductDTO(
            Long productId, String name, Long quantity, BigDecimal total){
        return new TopProductDTO(productId, name, quantity, total);
    }

    //Distribución por método de pago
    public static PaymentMethodDTO toPaymentMethodDTO(
            PaymentMethod method, Long count, BigDecimal total){
        String label = switch (method) {
            case CASH -> "Efectivo";
            case DEBIT -> "Tarjeta débito";
            case CREDIT -> "Tarjeta crédito";
        };

        return new PaymentMethodDTO(method.name(), label, count, total);
    }

    //Rendimiento por categoría
    public static CategoryPerformanceDTO toCategoryPerformanceDTO(
            Long categoryId, String name, Long quantity, BigDecimal total){
        String label = name == null || name.isBlank() ? "Sin categoría" : name;
        return new CategoryPerformanceDTO(categoryId, label, quantity, total);
    }

    //Producto con stock bajo
    public static LowStockDTO toLowStockDTO(ProductEntity product){
        String category = product.getCategory() == null
                ? null
                : product.getCategory().getName();

        return new LowStockDTO(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getBarcode(),
            category,
            product.getStock());
    }

    //Margen de un producto (costo real vs precio de venta)
    public static MarginDTO toMarginDTO(
            Long productId, String name, String sku,
            BigDecimal cost, BigDecimal price, BigDecimal margin, BigDecimal marginPercent){
        return new MarginDTO(productId, name, sku, cost, price, margin, marginPercent);
    }
}
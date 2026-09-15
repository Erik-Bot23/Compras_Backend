package com.erikjarquin.ventas.service;

import java.time.LocalDate;
import java.util.List;

import com.erikjarquin.ventas.model.dto.Reports.CategoryPerformanceDTO;
import com.erikjarquin.ventas.model.dto.Reports.LowStockDTO;
import com.erikjarquin.ventas.model.dto.Reports.MarginDTO;
import com.erikjarquin.ventas.model.dto.Reports.PaymentMethodDTO;
import com.erikjarquin.ventas.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.ventas.model.dto.Reports.ReportsSummaryDTO;
import com.erikjarquin.ventas.model.dto.Reports.TopProductDTO;
import com.erikjarquin.ventas.model.enums.ReportGroup;

/**
 * Contrato del módulo de REPORTES: agregados SQL de ventas e inventario que
 * consume el dashboard del frontend (que solo grafica). Ver
 * {@code service/impl/ReportsImpl}.
 */
public interface ReportsService {
    List<PeriodSalesDTO> getSalesTrend(LocalDate from, LocalDate to, ReportGroup groupBy);

    List<TopProductDTO> getTopProducts(LocalDate from, LocalDate to, int limit);

    List<PaymentMethodDTO> getPaymentMethodDistribution(LocalDate from, LocalDate to);

    List<CategoryPerformanceDTO> getCategoryPerformance(LocalDate from, LocalDate to);

    List<LowStockDTO> getLowStock(int threshold);

    //Márgenes por producto (costo real de compras vs precio de venta)
    List<MarginDTO> getMargins();

    ReportsSummaryDTO getSummary(LocalDate from, LocalDate to);
}
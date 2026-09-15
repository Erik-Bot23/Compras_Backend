package com.erikjarquin.ventas.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Reports.CategoryPerformanceDTO;
import com.erikjarquin.ventas.model.dto.Reports.LowStockDTO;
import com.erikjarquin.ventas.model.dto.Reports.MarginDTO;
import com.erikjarquin.ventas.model.dto.Reports.PaymentMethodDTO;
import com.erikjarquin.ventas.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.ventas.model.dto.Reports.ReportsSummaryDTO;
import com.erikjarquin.ventas.model.dto.Reports.TopProductDTO;
import com.erikjarquin.ventas.model.enums.ReportGroup;
import com.erikjarquin.ventas.service.ReportsService;

/**
 * Reportes (dashboard analítico). Todos los endpoints exigen el permiso
 * VER_REPORTES (la exportación PDF/Excel se hace del lado del cliente).
 *
 * <p>Todas las consultas aceptan {@code from}/{@code to} (ISO yyyy-MM-dd,
 * opcionales) y agregan en SQL.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportsService reportsService;

    public ReportController(ReportsService reportsService){
        this.reportsService = reportsService;
    }

    //Tendencia de ventas por día/mes/año
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/trend")
    public List<PeriodSalesDTO> getSalesTrend(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "MONTH") ReportGroup groupBy){
        return reportsService.getSalesTrend(from, to, groupBy);
    }

    //Productos más vendidos
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/top-products")
    public List<TopProductDTO> getTopProducts(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "5") int limit){
        return reportsService.getTopProducts(from, to, limit);
    }

    //Distribución por método de pago
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/payment-methods")
    public List<PaymentMethodDTO> getPaymentMethodDistribution(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to){
        return reportsService.getPaymentMethodDistribution(from, to);
    }

    //Rendimiento por categoría
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/categories")
    public List<CategoryPerformanceDTO> getCategoryPerformance(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to){
        return reportsService.getCategoryPerformance(from, to);
    }

    //Stock bajo (inventario)
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/low-stock")
    public List<LowStockDTO> getLowStock(@RequestParam(defaultValue = "10") int threshold){
        return reportsService.getLowStock(threshold);
    }

    //Márgenes por producto (costo real de compras vs precio de venta),
    //ordenados de menor a mayor margen (oportunidades de renegociación)
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/margins")
    public List<MarginDTO> getMargins(){
        return reportsService.getMargins();
    }

    //Resumen del rango (tarjetas del dashboard)
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @GetMapping("/summary")
    public ReportsSummaryDTO getSummary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to){
        return reportsService.getSummary(from, to);
    }
}
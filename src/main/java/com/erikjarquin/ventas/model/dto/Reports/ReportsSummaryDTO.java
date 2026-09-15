package com.erikjarquin.ventas.model.dto.Reports;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen global del rango ({@code GET /api/reports/summary}): total vendido,
 * número de ventas APPROVED, ticket promedio y desglose por método de pago.
 * Es la fuente de las tarjetas superiores del dashboard.
 */
public class ReportsSummaryDTO {
    private Long totalSales;
    private BigDecimal totalAmount;
    private BigDecimal averageTicket;
    private List<PaymentMethodDTO> paymentMethods;

    public ReportsSummaryDTO(){}

    public ReportsSummaryDTO(
        Long totalSales,
        BigDecimal totalAmount,
        BigDecimal averageTicket,
        List<PaymentMethodDTO> paymentMethods){
        this.totalSales = totalSales;
        this.totalAmount = totalAmount;
        this.averageTicket = averageTicket;
        this.paymentMethods = paymentMethods;
    }

    public Long getTotalSales(){ return totalSales; }
    public void setTotalSales(Long totalSales){ this.totalSales = totalSales; }

    public BigDecimal getTotalAmount(){ return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount){ this.totalAmount = totalAmount; }

    public BigDecimal getAverageTicket(){ return averageTicket; }
    public void setAverageTicket(BigDecimal averageTicket){ this.averageTicket = averageTicket; }

    public List<PaymentMethodDTO> getPaymentMethods(){ return paymentMethods; }
    public void setPaymentMethods(List<PaymentMethodDTO> paymentMethods){ this.paymentMethods = paymentMethods; }
}
package com.erikjarquin.ventas.model.dto.Reports;

import java.math.BigDecimal;

/**
 * Distribución de ventas por método de pago ({@code GET
 * /api/reports/payment-methods}). {@code method} es el nombre del enum
 * (CASH/DEBIT/CREDIT) y {@code label} un texto legible para la UI.
 */
public class PaymentMethodDTO {
    private String method;
    private String label;
    private Long count;
    private BigDecimal total;

    public PaymentMethodDTO(){}

    public PaymentMethodDTO(String method, String label, Long count, BigDecimal total){
        this.method = method;
        this.label = label;
        this.count = count;
        this.total = total;
    }

    public String getMethod(){ return method; }
    public void setMethod(String method){ this.method = method; }

    public String getLabel(){ return label; }
    public void setLabel(String label){ this.label = label; }

    public Long getCount(){ return count; }
    public void setCount(Long count){ this.count = count; }

    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total = total; }
}
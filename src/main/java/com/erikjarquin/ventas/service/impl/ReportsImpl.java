package com.erikjarquin.ventas.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.ventas.mapper.ReportsMapper;
import com.erikjarquin.ventas.model.dto.Reports.CategoryPerformanceDTO;
import com.erikjarquin.ventas.model.dto.Reports.LowStockDTO;
import com.erikjarquin.ventas.model.dto.Reports.MarginDTO;
import com.erikjarquin.ventas.model.dto.Reports.PaymentMethodDTO;
import com.erikjarquin.ventas.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.ventas.model.dto.Reports.ReportsSummaryDTO;
import com.erikjarquin.ventas.model.dto.Reports.TopProductDTO;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.enums.PaymentMethod;
import com.erikjarquin.ventas.model.enums.PaymentStatus;
import com.erikjarquin.ventas.model.enums.ReportGroup;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.repository.SaleRepository;
import com.erikjarquin.ventas.service.ReportsService;

/**
 * Implementación de reportes. Todas las consultas agregan EN SQL (a través de
 * SaleRepository/ProductRepository) y aquí solo se transforman a DTO, de forma
 * que el frontend reciba ya agregado y solo tenga que graficar.
 *
 * <p>Se contabilizan únicamente ventas con paymentStatus APPROVED (excluye
 * PENDING el monitor está atendiendo, REJECTED y REVERSED).
 */
@Service
public class ReportsImpl implements ReportsService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public ReportsImpl(SaleRepository saleRepository, ProductRepository productRepository){
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    //Tendencia de ventas agrupada por día/mes/año
    @Override
    @Transactional(readOnly = true)
    public List<PeriodSalesDTO> getSalesTrend(LocalDate from, LocalDate to, ReportGroup groupBy){
        Range range = resolveRange(from, to);
        List<Object[]> rows = switch (groupBy) {
            case DAY -> saleRepository.sumSalesByDay(range.from(), range.to(), PaymentStatus.APPROVED);
            case MONTH -> saleRepository.sumSalesByMonth(range.from(), range.to(), PaymentStatus.APPROVED);
            case YEAR -> saleRepository.sumSalesByYear(range.from(), range.to(), PaymentStatus.APPROVED);
        };

        List<PeriodSalesDTO> result = new ArrayList<>();
        for(Object[] row : rows){
            String period = toPeriodLabel(groupBy, row);
            BigDecimal total = toBigDecimal(row[1 + periodOffset(groupBy)]);
            Long count = toLong(row[2 + periodOffset(groupBy)]);
            result.add(ReportsMapper.toPeriodSalesDTO(period, total, count));
        }
        return result;
    }

    //Los N productos más vendidos
    @Override
    @Transactional(readOnly = true)
    public List<TopProductDTO> getTopProducts(LocalDate from, LocalDate to, int limit){
        if(limit < 1 || limit > 100){
            throw new IllegalArgumentException("El límite debe estar entre 1 y 100");
        }

        Range range = resolveRange(from, to);
        List<Object[]> rows = saleRepository.findTopSellingProducts(
                range.from(), range.to(), PaymentStatus.APPROVED, PageRequest.of(0, limit));

        List<TopProductDTO> result = new ArrayList<>();
        for(Object[] row : rows){
            result.add(ReportsMapper.toTopProductDTO(
                    toLong(row[0]),
                    (String) row[1],
                    toLong(row[2]),
                    toBigDecimal(row[3])));
        }
        return result;
    }

    //Distribución por método de pago
    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getPaymentMethodDistribution(LocalDate from, LocalDate to){
        Range range = resolveRange(from, to);
        List<Object[]> rows = saleRepository.findPaymentMethodDistribution(
                range.from(), range.to(), PaymentStatus.APPROVED);

        List<PaymentMethodDTO> result = new ArrayList<>();
        for(Object[] row : rows){
            PaymentMethod method = (PaymentMethod) row[0];
            result.add(ReportsMapper.toPaymentMethodDTO(
                    method, toLong(row[2]), toBigDecimal(row[1])));
        }
        return result;
    }

    //Rendimiento por categoría
    @Override
    @Transactional(readOnly = true)
    public List<CategoryPerformanceDTO> getCategoryPerformance(LocalDate from, LocalDate to){
        Range range = resolveRange(from, to);
        List<Object[]> rows = saleRepository.findCategoryPerformance(
                range.from(), range.to(), PaymentStatus.APPROVED);

        List<CategoryPerformanceDTO> result = new ArrayList<>();
        for(Object[] row : rows){
            result.add(ReportsMapper.toCategoryPerformanceDTO(
                    (Long) row[0], (String) row[1], toLong(row[2]), toBigDecimal(row[3])));
        }
        return result;
    }

    //Productos con stock bajo
    @Override
    @Transactional(readOnly = true)
    public List<LowStockDTO> getLowStock(int threshold){
        if(threshold < 0){
            throw new IllegalArgumentException("El umbral de stock no puede ser negativo");
        }

        return productRepository.findLowStock(threshold).stream()
                .map(ReportsMapper::toLowStockDTO)
                .toList();
    }

    //Márgenes por producto: cost (último costo real de compras) vs price.
    //Se ordenan de MENOR a MAYOR margen: primero los que conviene renegociar.
    @Override
    @Transactional(readOnly = true)
    public List<MarginDTO> getMargins(){
        return productRepository.findAll().stream()
                .map(product -> {
                    BigDecimal cost = product.getCost() != null ? product.getCost() : BigDecimal.ZERO;
                    BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                    BigDecimal margin = price.subtract(cost).setScale(2, RoundingMode.HALF_UP);

                    //%= margin/price*100; si no hay precio de venta no se puede calcular
                    BigDecimal marginPercent = price.signum() > 0
                            ? margin.multiply(BigDecimal.valueOf(100))
                                    .divide(price, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return ReportsMapper.toMarginDTO(
                            product.getId(), product.getName(), product.getSku(),
                            cost, price, margin, marginPercent);
                })
                .sorted(Comparator.comparing(MarginDTO::getMarginPercent))
                .toList();
    }

    //Resumen del rango (tarjetas del dashboard)
    @Override
    @Transactional(readOnly = true)
    public ReportsSummaryDTO getSummary(LocalDate from, LocalDate to){
        Range range = resolveRange(from, to);

        List<Object[]> totalsRows = saleRepository.sumSalesTotals(
                range.from(), range.to(), PaymentStatus.APPROVED);

        Long totalSales = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal averageTicket = BigDecimal.ZERO;

        if(!totalsRows.isEmpty()){
            Object[] row = totalsRows.get(0);
            totalSales = toLong(row[0]);
            totalAmount = toBigDecimal(row[1]);
            averageTicket = toBigDecimal(row[2]).setScale(2, RoundingMode.HALF_UP);
        }

        List<PaymentMethodDTO> methods = getPaymentMethodDistribution(from, to);

        return new ReportsSummaryDTO(totalSales, totalAmount, averageTicket, methods);
    }

    //-------------------------- Helpers ---------------------------

    //Rango de fechas con valores por defecto y validación
    private Range resolveRange(LocalDate from, LocalDate to){
        LocalDate start = from == null ? LocalDate.of(2000, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;

        if(end.isBefore(start)){
            throw new IllegalArgumentException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }

        return new Range(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    //Etiqueta ISO del periodo según la agrupación
    private String toPeriodLabel(ReportGroup groupBy, Object[] row){
        return switch (groupBy) {
            case DAY -> String.valueOf(row[0]);
            case MONTH -> {
                int year = ((Number) row[0]).intValue();
                int month = ((Number) row[1]).intValue();
                yield String.format("%04d-%02d", year, month);
            }
            case YEAR -> String.valueOf(((Number) row[0]).intValue());
        };
    }

    //Índices de las columnas SUM/COUNT según la agrupación
    private int periodOffset(ReportGroup groupBy){
        return switch (groupBy) {
            case DAY, YEAR -> 0;
            case MONTH -> 1;
        };
    }

    private Long toLong(Object value){
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value){
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    //Rango interno (from inclusive, to exclusivo)
    private record Range(LocalDateTime from, LocalDateTime to) {}
}
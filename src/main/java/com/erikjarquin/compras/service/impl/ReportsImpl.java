package com.erikjarquin.compras.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.compras.exceptions.ReportException;
import com.erikjarquin.compras.mapper.ReportsMapper;
import com.erikjarquin.compras.mapper.SaleMapper;
import com.erikjarquin.compras.model.dto.Reports.CashReportDTO;
import com.erikjarquin.compras.model.dto.Reports.CategoryPerformanceDTO;
import com.erikjarquin.compras.model.dto.Reports.LowStockDTO;
import com.erikjarquin.compras.model.dto.Reports.MarginDTO;
import com.erikjarquin.compras.model.dto.Reports.PaymentMethodDTO;
import com.erikjarquin.compras.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.compras.model.dto.Reports.ProfitDTO;
import com.erikjarquin.compras.model.dto.Reports.ReportsSummaryDTO;
import com.erikjarquin.compras.model.dto.Reports.TopProductDTO;
import com.erikjarquin.compras.model.entity.CashRegisterEntity;
import com.erikjarquin.compras.model.entity.SaleDetailEntity;
import com.erikjarquin.compras.model.entity.SaleEntity;
import com.erikjarquin.compras.model.enums.PaymentMethod;
import com.erikjarquin.compras.model.enums.PaymentStatus;
import com.erikjarquin.compras.model.enums.ReportGroup;
import com.erikjarquin.compras.repository.CashRegisterRepository;
import com.erikjarquin.compras.repository.ProductRepository;
import com.erikjarquin.compras.repository.SaleRepository;
import com.erikjarquin.compras.service.ReportsService;

/**
 * Implementación de reportes. Todas las consultas agregan EN SQL (a través de
 * SaleRepository/ProductRepository) y aquí solo se transforman a DTO, de forma
 * que el frontend reciba ya agregado y solo tenga que graficar.
 *
 * Se contabilizan únicamente ventas con paymentStatus APPROVED (excluye
 * PENDING el monitor está atendiendo, REJECTED y REVERSED).
 *
 * Y además NO anuladas (V3, 2026-09-30). Anular una venta deja la
 * fila con {@code cancelled = true} a propósito, es evidencia contable pero
 * NO cambia el {@code paymentStatus}, que sigue en APPROVED. Filtrar solo por
 * APPROVED hacía que una venta anulada siguiera sumando ingresos; con la
 * utilidad nueva también habría summedo costo. Por eso las consultas de
 * {@link SaleRepository} llevan las dos condiciones.
 */
@Service
public class ReportsImpl implements ReportsService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final SaleMapper saleMapper;

    public ReportsImpl(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            CashRegisterRepository cashRegisterRepository,
            SaleMapper saleMapper){
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.saleMapper = saleMapper;
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

    /**
     * UTILIDAD del periodo (V3): ingresos − costo de lo vendido.
     *
     * El costo de lo vendido sale de {@code sumCostOfGoodsSold}, que suma
     * {@code quantity * unitCost} de los renglones: el costo congelado en cada
     * venta. No se usa {@code product.cost} porque es el del último purchase de
     * hoy; comprar algo más barato mañana cambiaría la utilidad de ayer.
     *
     * El margen se calcula sobre VENTA, que es como se mide el markup de un
     * comercio. Se devuelve 0 (y no se divide entre cero) cuando no hubo
     * ingresos en el periodo.
     */
    @Override
    @Transactional(readOnly = true)
    public ProfitDTO getProfit(LocalDate from, LocalDate to){
        Range range = resolveRange(from, to);

        List<Object[]> totalsRows = saleRepository.sumSalesTotals(
                range.from(), range.to(), PaymentStatus.APPROVED);
        List<Object[]> cogsRows = saleRepository.sumCostOfGoodsSold(
                range.from(), range.to(), PaymentStatus.APPROVED);

        long tickets = 0L;
        BigDecimal revenue = BigDecimal.ZERO;

        if(!totalsRows.isEmpty()){
            Object[] row = totalsRows.get(0);
            tickets = toLong(row[0]);
            revenue = toBigDecimal(row[1]).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cogs = BigDecimal.ZERO;
        long itemsSold = 0L;
        long itemsWithoutCost = 0L;

        if(!cogsRows.isEmpty()){
            Object[] row = cogsRows.get(0);
            cogs = toBigDecimal(row[0]).setScale(2, RoundingMode.HALF_UP);
            itemsWithoutCost = toLong(row[1]);
            itemsSold = toLong(row[2]);
        }

        BigDecimal grossProfit = revenue.subtract(cogs).setScale(2, RoundingMode.HALF_UP);

        BigDecimal marginPercent = revenue.signum() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : grossProfit.multiply(BigDecimal.valueOf(100))
                        .divide(revenue, 2, RoundingMode.HALF_UP);

        return new ProfitDTO(revenue, cogs, grossProfit, marginPercent, tickets, itemsSold, itemsWithoutCost);
    }

    /**
     * Detalle de UNA caja (V3): sus ventas y su utilidad.
     *
     * Las ventas se leen por {@code cash_register_id} y se filtran las
     * anuladas. El costo se calcula recorriendo los renglones en vez de usar una
     * consulta agregada porque aquí el rango es una caja, no fechas: el filtro
     * por fechas no aplica y forzar la agenda "openedAt a closedAt" dejaría fuera
     * las ventas de una caja abierta.
     */
    @Override
    @Transactional(readOnly = true)
    public CashReportDTO getCashReport(Long cashId){
        CashRegisterEntity cash = cashRegisterRepository.findById(cashId)
                .orElseThrow(() -> new ReportException("No existe la caja indicada", HttpStatus.NOT_FOUND));

        List<SaleEntity> sales = saleRepository.findValidSalesByCashId(cashId);

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal debitSales = BigDecimal.ZERO;
        BigDecimal creditSales = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;

        for(SaleEntity sale : sales){
            switch(sale.getPaymentMethod()){
                case CASH -> cashSales = cashSales.add(sale.getTotal());
                case DEBIT -> debitSales = debitSales.add(sale.getTotal());
                case CREDIT -> creditSales = creditSales.add(sale.getTotal());
            }

            if(sale.getDetails() != null){
                for(SaleDetailEntity detail : sale.getDetails()){
                    if(detail.getUnitCost() == null || detail.getQuantity() == null){
                        continue;
                    }
                    cogs = cogs.add(detail.getUnitCost()
                            .multiply(BigDecimal.valueOf(detail.getQuantity())));
                }
            }
        }

        BigDecimal totalSales = cashSales.add(debitSales).add(creditSales);
        BigDecimal expected = cash.getOpeningAmount() == null
                ? BigDecimal.ZERO
                : cash.getOpeningAmount().add(cashSales);

        CashReportDTO dto = new CashReportDTO();
        dto.setCashId(cash.getId());
        dto.setNumber(cash.getNumber());
        dto.setOpenedAt(cash.getOpenedAt());
        dto.setClosedAt(cash.getClosedAt());
        dto.setOpeningAmount(cash.getOpeningAmount());
        dto.setClosingAmount(cash.getCountedAmount());
        dto.setExpectedAmount(expected.setScale(2, RoundingMode.HALF_UP));
        dto.setDifference(cash.getDifference());
        dto.setCashSales(cashSales.setScale(2, RoundingMode.HALF_UP));
        dto.setDebitSales(debitSales.setScale(2, RoundingMode.HALF_UP));
        dto.setCreditSales(creditSales.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalSales(totalSales.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalTickets(sales.size());
        dto.setGrossProfit(totalSales.subtract(cogs).setScale(2, RoundingMode.HALF_UP));
        dto.setActive(Boolean.TRUE.equals(cash.getActive()));
        dto.setSales(sales.stream().map(saleMapper::toDetailResponse).toList());

        return dto;
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
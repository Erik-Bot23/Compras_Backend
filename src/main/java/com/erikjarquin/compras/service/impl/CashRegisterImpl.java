package com.erikjarquin.compras.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erikjarquin.compras.exceptions.CashException;
import com.erikjarquin.compras.mapper.CashRegisterMapper;
import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.compras.model.entity.CashRegisterEntity;
import com.erikjarquin.compras.model.entity.SaleEntity;
import com.erikjarquin.compras.repository.CashRegisterRepository;
import com.erikjarquin.compras.repository.SaleRepository;
import com.erikjarquin.compras.service.CashRegisterService;

/**
 * Implementación de la caja registradora.
 *
 * <p>Reglas: solo puede existir UNA caja abierta a la vez. Al cerrar se calcula
 * el resumen por método de pago (efectivo/débito/crédito) y la diferencia contra
 * el efectivo contado. Los errores de negocio se lanzan como CashException con
 * el HttpStatus adecuado (409 si ya hay caja abierta, 404 si no existe).
 */
@Service
public class CashRegisterImpl implements CashRegisterService {
    private final CashRegisterRepository repository;
    private final CashRegisterMapper mapper;
    private final SaleRepository saleRepository;

    public CashRegisterImpl(
        CashRegisterRepository repository,
        CashRegisterMapper mapper,
        SaleRepository saleRepository){
        this.repository = repository;
        this.mapper = mapper;
        this.saleRepository=saleRepository;
    }

    //Abrir caja
    @Override //
    public CashResponse open(OpenCashRequest request){
        repository.findByActiveTrue().ifPresent(c -> {
            throw new CashException("Ya existe una caja abierta", HttpStatus.CONFLICT);
        });

        //V3: el numero que escribe el vendedor es OBLIGATORIO y UNIQUE.
        //Es lo que despues permite "filtrar por caja" en Reportes. Se valida a
        //mano (no solo con el UNIQUE de la BD) para dar un 409 con mensaje claro
        //en vez de un 500 de constraint.
        String number = normalizeNumber(request.getNumber());

        if(repository.existsByNumber(number)){
            throw new CashException(
                "Ya existe una caja con el numero \"" + number + "\". "
                + "Cada caja necesita un numero unico para poder filtrar los reportes.",
                HttpStatus.CONFLICT);
        }

        CashRegisterEntity cash = new CashRegisterEntity();

        //Inicializar para evitar valores null
        cash.setCashSales(BigDecimal.ZERO);
        cash.setDebitSales(BigDecimal.ZERO);
        cash.setCreditSales(BigDecimal.ZERO);
        cash.setTotalSales(BigDecimal.ZERO);
        cash.setExpectedAmount(BigDecimal.ZERO);
        cash.setDifference(BigDecimal.ZERO);
        cash.setTotalTickets(0);

        cash.setNumber(number);
        cash.setOpenedAt(LocalDateTime.now());
        cash.setOpeningAmount(request.getOpeningAmount());
        cash.setActive(true);
        repository.save(cash);

        return mapper.toResponse(cash);
    }

    /**
     * Normaliza el numero de caja: recorta espacios y capitaliza.
     *
     * <p>Se capitaliza a proposito: "caja 1" y "CAJA 1" son la MISMA caja y
     * deben ser el mismo numero. Sin esto el UNIQUE de la BD los dejaria pasar
     * como dos cajas distintas (PostgreSQL compara con mayusculas y minusculas
     * distintas) y el reporte de esa caja mesclaria dos cortes reales.
     */
    private String normalizeNumber(String number){
        if(number == null || number.isBlank()){
            throw new CashException("El numero de caja es obligatorio", HttpStatus.BAD_REQUEST);
        }

        String normalized = number.trim().toUpperCase();

        if(normalized.length() > 50){
            throw new CashException(
                "El numero de caja no puede tener mas de 50 caracteres", HttpStatus.BAD_REQUEST);
        }

        return normalized;
    }

    //Historial de cajas (V3): la mas reciente primero, para elegir cual filtrar
    @Override
    public List<CashResponse> getHistory(){
        return repository.findAllByOrderByOpenedAtDesc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    //Detalle de una caja concreta por su numero (V3)
    @Override
    public CashResponse getByNumber(String number){
        return repository.findByNumber(normalizeNumber(number))
                .map(mapper::toResponse)
                .orElseThrow(() -> new CashException(
                    "No existe ninguna caja con el numero \"" + number + "\"", HttpStatus.NOT_FOUND));
    }

    //Cerrar la caja
    @Override
    public CashResponse close(CloseCashRequest request){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() -> 
            new CashException("No existe caja abierta"));

        CashSummaryResponse summary = calculateSummary(cash);

        //Diferencia
        BigDecimal countedAmount = request.getClosingAmount();
        BigDecimal difference = countedAmount.subtract(summary.getExpectedAmount());

        //Guardar todo
        cash.setCountedAmount(countedAmount);
        cash.setCashSales(summary.getCashSales());
        cash.setDebitSales(summary.getDebitSales());
        cash.setCreditSales(summary.getCreditSales());
        cash.setTotalSales(summary.getTotalSales());
        cash.setExpectedAmount(summary.getExpectedAmount());
        cash.setDifference(difference);
        cash.setTotalTickets(summary.getTotalTickets());
        cash.setClosedAt(LocalDateTime.now());
        cash.setActive(false);
        repository.save(cash);

        return mapper.toResponse(cash);
    }

    //Ver la caja activa
    @Override
    public CashResponse getActiveCash(){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() ->
            new CashException("No existe la caja abierta"));

        return mapper.toResponse(cash);
    }

    //Ver el resumen de la venta
    @Override
    public CashSummaryResponse getSummary(){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() ->
            new CashException("No existe caja abierta"));

        return calculateSummary(cash);
    }

    //Calcular el resumen de la venta
    private CashSummaryResponse calculateSummary(CashRegisterEntity cash){
        List<SaleEntity> sales = saleRepository.findByCashRegister(cash);

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal debitSales = BigDecimal.ZERO;
        BigDecimal creditSales = BigDecimal.ZERO;

        for(SaleEntity sale : sales){
            //V3: se ignoran las ventas ANULADAS (cancelar no cambia el
            //paymentStatus, que sigue APPROVED). Sin este filtro, anular una
            //venta en efectivo dejaba su dinero dentro del "esperado" del corte
            //y el cajero sortiesaba una diferencia fantasma. Mismo motivo que el
            //`cancelled = false` de las consultas de Reportes.
            if(sale.isCancelled()){
                continue;
            }

            switch(sale.getPaymentMethod()){
                case CASH -> cashSales = cashSales.add(sale.getTotal());

                case DEBIT -> debitSales = debitSales.add(sale.getTotal());

                case CREDIT -> creditSales = creditSales.add(sale.getTotal());
            }
        }

        BigDecimal totalSales = cashSales.add(debitSales).add(creditSales);
        BigDecimal expectedAmount = cash.getOpeningAmount().add(cashSales);

        CashSummaryResponse response = new CashSummaryResponse();
        response.setCashId(cash.getId());
        response.setOpeningAmount(cash.getOpeningAmount());
        response.setCashSales(cashSales);
        response.setDebitSales(debitSales);
        response.setCreditSales(creditSales);
        response.setTotalSales(totalSales);
        response.setExpectedAmount(expectedAmount);
        response.setTotalTickets(sales.size());

        return response;
    }
}

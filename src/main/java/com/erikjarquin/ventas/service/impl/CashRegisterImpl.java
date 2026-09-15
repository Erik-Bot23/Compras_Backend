package com.erikjarquin.ventas.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.exceptions.CashException;
import com.erikjarquin.ventas.mapper.CashRegisterMapper;
import com.erikjarquin.ventas.model.dto.Cash.CashResponse;
import com.erikjarquin.ventas.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.ventas.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.ventas.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.ventas.model.entity.CashRegisterEntity;
import com.erikjarquin.ventas.model.entity.SaleEntity;
import com.erikjarquin.ventas.repository.CashRegisterRepository;
import com.erikjarquin.ventas.repository.SaleRepository;
import com.erikjarquin.ventas.service.CashRegisterService;

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

        CashRegisterEntity cash = new CashRegisterEntity();

        //Inicializar para evitar valores null
        cash.setCashSales(BigDecimal.ZERO);
        cash.setDebitSales(BigDecimal.ZERO);
        cash.setCreditSales(BigDecimal.ZERO);
        cash.setTotalSales(BigDecimal.ZERO);
        cash.setExpectedAmount(BigDecimal.ZERO);
        cash.setDifference(BigDecimal.ZERO);
        cash.setTotalTickets(0);

        cash.setOpenedAt(LocalDateTime.now());
        cash.setOpeningAmount(request.getOpeningAmount());
        cash.setActive(true);
        repository.save(cash);

        return mapper.toResponse(cash);
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

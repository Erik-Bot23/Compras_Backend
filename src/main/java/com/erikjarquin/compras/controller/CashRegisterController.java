package com.erikjarquin.compras.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.compras.service.CashRegisterService;

/**
 * Caja registradora: apertura, cierre, resumen y consulta de caja activa.
 *
 * <p>Permisos: ABRIR_CAJA, CERRAR_CAJA, VER_CAJA, CORTE_CAJA.
 * CORS global (${CORS_ALLOWED_ORIGINS}) en SecurityConfig.
 */
@RestController
@RequestMapping("/api/local/cash")
public class CashRegisterController {
    private final CashRegisterService service;

    public CashRegisterController(CashRegisterService service){
        this.service=service;
    }

    //Ver resumen de las ventas
    @PreAuthorize("hasAuthority('CORTE_CAJA')")
    @GetMapping("/summary")
    public CashSummaryResponse getSummary(){
        return service.getSummary();
    }

    //Abrir caja
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    @PostMapping("/open")
    public CashResponse open(@RequestBody OpenCashRequest request){
        return service.open(request);
    }

    //Cerrar la caja
    @PreAuthorize("hasAuthority('CERRAR_CAJA')")
    @PostMapping("/close")
    public CashResponse close(@RequestBody CloseCashRequest request){
        return service.close(request);
    }

    //Ver si la caja esta activa
    @PreAuthorize("hasAuthority('VER_CAJA')")
    @GetMapping("/active")
    public CashResponse getActiveCash(){
        return service.getActiveCash();
    }

    /**
     * Historial de cajas (V3): la mas reciente primero. Es lo que alimenta el
     * selector "filtrar por caja" de Reportes, asi que se cuelga de
     * {@code VER_CAJA} y no de {@code CORTE_CAJA}: ver una lista de cajas es
     * consultar, no hacer un corte.
     */
    @PreAuthorize("hasAuthority('VER_CAJA')")
    @GetMapping("/history")
    public List<CashResponse> getHistory(){
        return service.getHistory();
    }

    //Detalle de una caja por su numero (V3)
    @PreAuthorize("hasAuthority('VER_CAJA')")
    @GetMapping("/number/{number}")
    public CashResponse getByNumber(@PathVariable String number){
        return service.getByNumber(number);
    }

}

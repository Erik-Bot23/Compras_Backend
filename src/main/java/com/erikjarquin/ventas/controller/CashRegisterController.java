package com.erikjarquin.ventas.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Cash.CashResponse;
import com.erikjarquin.ventas.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.ventas.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.ventas.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.ventas.service.CashRegisterService;

/**
 * Caja registradora: apertura, cierre, resumen y consulta de caja activa.
 *
 * <p>Permisos: ABRIR_CAJA, CERRAR_CAJA, VER_CAJA, CORTE_CAJA.
 * CORS global (${CORS_ALLOWED_ORIGINS}) en SecurityConfig.
 */
@RestController
@RequestMapping("/api/cash")
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

}

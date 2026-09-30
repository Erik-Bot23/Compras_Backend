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
import com.erikjarquin.compras.model.dto.Cash.CreateCashRequest;
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

    /**
     * CREAR una caja física (V3). La caja queda registrada pero NO abierta: se
     * abre después eligiéndola en {@code POST /open}.
     *
     * <p>Se separa de "abrir" porque el número tiene que existir antes para poder
     * elegirse. Un 409 si el número ya existe: sin unicidad, dos cortes distintos
     * se mezclarían al filtrar reportes por caja.
     */
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    @PostMapping
    public CashResponse create(@RequestBody CreateCashRequest request){
        return service.create(request);
    }

    /**
     * Cajas todavía sin abrir: las que se pueden elegir para abrir.
     *
     * <p>Usa {@code ABRIR_CAJA} y no {@code VER_CAJA} porque es el insumo directo
     * de "abrir caja": quien puede abrir necesita saber qué cajas hay.
     */
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    @GetMapping("/available")
    public List<CashResponse> getAvailable(){
        return service.getAvailable();
    }

    /**
     * Sugiere el siguiente número libre ("CAJA n") para prellenar el modal de
     * crear caja. Es solo una sugerencia: el usuario puede escribir otro.
     */
    @PreAuthorize("hasAuthority('ABRIR_CAJA')")
    @GetMapping("/next-number")
    public NextNumberResponse getNextNumber(){
        return new NextNumberResponse(service.getNextSuggestedNumber());
    }

    //Abrir una caja ya registrada
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

    /**
     * Envoltorio de {@code GET /cash/next-number}: un endpoint que devuelve un
     * String pelado es raro de consumir desde TypeScript, que espera un objeto
     * con la propiedad nombrada.
     */
    public record NextNumberResponse(String suggestedNumber){}

}

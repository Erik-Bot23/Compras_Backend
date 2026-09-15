package com.erikjarquin.ventas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Payment.CardPaymentRequest;
import com.erikjarquin.ventas.model.dto.Payment.CardPaymentResponse;
import com.erikjarquin.ventas.service.PaymentService;

import lombok.extern.slf4j.Slf4j;

/**
 * Pagos con tarjeta a través de la terminal (simulada o física).
 *
 * <p>Exposa: cobro (POST /card), consulta de estado (GET /status/{id}),
 * reintento de pago rechazado (POST /retry/{id}) y reversa de pago aprobado
 * (POST /reverse/{id}). Todos exigen el permiso PROCESAR_PAGOS.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService=paymentService;
    }

    //Procesar el pago con tarjeta
    @PreAuthorize("hasAuthority('PROCESAR_PAGOS')")
    @PostMapping("/card")
    public ResponseEntity<CardPaymentResponse> processCardPayment(@RequestBody CardPaymentRequest request){
        log.info("Solicitud de pago con tarjeta recibida");
        CardPaymentResponse response = paymentService.processCardPayment(request);
        return ResponseEntity.ok(response);
    }

    //Consultar estado por transactionId
    @PreAuthorize("hasAuthority('PROCESAR_PAGOS')")
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<CardPaymentResponse> getPaymentStatus(@PathVariable String transactionId){
        log.info("Consultando estado de transacción");
        CardPaymentResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(response);
    }

    //Reintentar pago
    @PreAuthorize("hasAuthority('PROCESAR_PAGOS')")
    @PostMapping("/retry/{paymentId}")
    public ResponseEntity<CardPaymentResponse> retryPayment(@PathVariable Long paymentId){
        log.info("Reintentando pago ID: {}", paymentId);
        CardPaymentResponse response = paymentService.retryPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    //Regresar pago si falla
    @PreAuthorize("hasAuthority('PROCESAR_PAGOS')")
    @PostMapping("/reverse/{paymentId}")
    public ResponseEntity<Boolean> reversePayment(@PathVariable Long paymentId){
        log.info("Reversando pago ID: {}", paymentId);
        boolean reversed = paymentService.reversePayment(paymentId);
        return ResponseEntity.ok(reversed);
    }
}

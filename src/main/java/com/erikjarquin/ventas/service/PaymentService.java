package com.erikjarquin.ventas.service;

import com.erikjarquin.ventas.model.dto.Payment.CardPaymentRequest;
import com.erikjarquin.ventas.model.dto.Payment.CardPaymentResponse;

/**
 * Contrato de pagos con tarjeta: cobro, consulta de estado, reintento y reversa.
 * Ver {@code service/impl/PaymentImpl}.
 */
public interface PaymentService {
    CardPaymentResponse processCardPayment(CardPaymentRequest request);
    //Consultar estado por transactionId
    CardPaymentResponse getPaymentStatus(String transactionId);
    //Reintentar pago fallido
    CardPaymentResponse retryPayment(Long paymentId);
    //Método de reversa
    boolean reversePayment(Long paymentId);
} 
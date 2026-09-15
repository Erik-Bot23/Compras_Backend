package com.erikjarquin.ventas.jobs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.PaymentEntity;
import com.erikjarquin.ventas.model.enums.PaymentStatus;
import com.erikjarquin.ventas.repository.PaymentRepository;
import com.erikjarquin.ventas.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que vigila los pagos con tarjeta que quedaron en estado
 * PENDING durante mucho tiempo.
 *
 * <p>¿Por qué existe? Si la terminal aprueba el cobro pero el cliente pierde
 * la conexión antes de recibir la respuesta, el pago queda PENDING en la BD.
 * Este job consulta la terminal cada 5 minutos (habilitado con @EnableScheduling
 * en VentasApplication) para resolver ese pagos y actualizarlos a APPROVED/REJECTED.
 *
 * <p>Ubicado en el paquete {@code jobs} (no en DTOs): es una tarea de
 * infraestructura, no un objeto de transferencia de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMonitorJob {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Se ejecuta cada 5 minutos (fixedDelay = 300000 ms).
     * Busca pagos PENDING con más de 5 minutos de antigüedad y consulta su
     * estado real contra la terminal, actualizando la BD en consecuencia.
     */
    @Scheduled(fixedDelay = 300000)
    public void monitorPendingPayments() {
        log.info("Monitoreando pagos pendientes...");

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<PaymentEntity> pendingPayments =
                paymentRepository.findByStatusAndPaymentDateBefore(PaymentStatus.PENDING, fiveMinutesAgo);

        if (pendingPayments.isEmpty()) {
            log.info("No hay pagos pendientes por monitorear");
            return;
        }

        log.info("Encontrados {} pagos pendientes", pendingPayments.size());

        for (PaymentEntity payment : pendingPayments) {
            try {
                log.info("Consultando estado de pago: {}", payment.getTransactionId());
                paymentService.getPaymentStatus(payment.getTransactionId());

                // Pequeña pausa para no sobrecargar la terminal.
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Error monitoreando pago {}: {}", payment.getTransactionId(), e.getMessage());
            }
        }
    }
}
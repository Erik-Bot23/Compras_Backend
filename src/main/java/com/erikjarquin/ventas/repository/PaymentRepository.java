package com.erikjarquin.ventas.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.ventas.model.entity.PaymentEntity;
import com.erikjarquin.ventas.model.enums.PaymentStatus;

/**
 * Repositorio de pagos.
 *
 * <p>- findByTransactionId: soporta la idempotencia (no duplicar un pago que
 *   ya fue emitido con el mismo transactionId).
 * - findByStatusAndStatusQueriedFalse: pagos PENDING que aún no se consultaron.
 * - findByStatusAndPaymentDateBefore: pagos PENDING y vetustos (> X minutos) →
 *   los detecta PaymentMonitorJob para marcarlos REJECTED.
 */
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findBySaleId(Long saleId);

    //Buscar por transactionId
    Optional<PaymentEntity> findByTransactionId(String transactionId);

    //Pagos pendientes para monitoreo
    List<PaymentEntity> findByStatusAndStatusQueriedFalse(PaymentStatus status);

    //Pagos pendientes por más de X tiempo
    List<PaymentEntity> findByStatusAndPaymentDateBefore(PaymentStatus status, LocalDateTime date);    
}

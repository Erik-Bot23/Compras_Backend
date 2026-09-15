package com.erikjarquin.ventas.service;

import com.erikjarquin.ventas.model.dto.Terminal.TerminalRequest;
import com.erikjarquin.ventas.model.dto.Terminal.TerminalResponse;

/**
 * Abstracción del terminal de pagos. Dos implementaciones:
 *  - {@code TimerSimulatedImpl} (pruebas, tarjetas deterministas/aleatorias)
 *  - {@code TerminalPhysicalImpl} (terminal real por Socket TCP, protocolo PAY|/REV|/STS|)
 * Se elige según la propiedad {@code payment.terminal.type} (SIMULATED | PHYSICAL).
 */
public interface TerminalService {
    /**
     * Procesa un pago a través de la terminal
     * @param request Datos de la transacción
     * @return Respuesta de la terminal
     */
    TerminalResponse processPayment(TerminalRequest request);
    /**
     * Reversa/ Cancela una transacción
     * @param transactionId ID de transacción original
     * @return true si se canceló exitosamente
     */
    boolean reversePayment(String transactionId);
    /**
     * Consulta el estado de una transacción
     * @param transactionId ID de transacción
     * @return Estado de la transacción
     */
    TerminalResponse getTransactionStatus(String transactionId);
}

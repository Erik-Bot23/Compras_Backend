package com.erikjarquin.compras.model.dto.Terminal;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalResponse {
    private boolean approved; //¿Aprovado?
    private String authorizationCode; //Código de autorización
    private String lastFourDigits; //últimos 4 dígitos 
    private String transactionId; // ID de la transacción
    private String cardBrand; // Marca de la tarjeta (Visa, MC)
    private String cardType; // CREDIT O DEBIT
    private String responseCode; // Código de respuesta del banco
    private String responseMessage; // Mensaje del banco
    private LocalDateTime transactionDate; // Fecha/hora de la transacción
    private String errorMessage; // si hubo error
}

package com.erikjarquin.compras.model.dto.Terminal;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCard {
    private String cardNumber; //Número de tarjeta ficticio
    private String brand; //VISA, MASTERCARD
    private String cardType; //DEBIT, CREDIT
    private BigDecimal balance; //Saldo disponible
    private String status; //ACTIVE, BLOCKED
    private String pin;//Pin correcto (para simulación)
    private String errorCode;//Código de error si aplica
    private String errorMessage;//Mensaje de error
}

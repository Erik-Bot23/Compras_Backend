package com.erikjarquin.ventas.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.erikjarquin.ventas.model.dto.Terminal.TestCard;

import lombok.extern.slf4j.Slf4j;

/**
 * Repositorio de tarjetas de PRUEBA (solo usado por TerminalSimulatedImpl).
 *
 * <p>⚠️ En memoria (NO persiste): vive en el MAP con tarjetas determinísticas
 * para simular casos: saldo suficiente/insuficiente, bloqueada, PIN inválido
 * y crédito. En producción con TERMINAL_TYPE=PHYSICAL este repo no se usa.
 * NUNCA guardar datos reales de tarjetas aquí.
 */
@Slf4j
@Repository
public class TestCardRepository {
    private final Map<String, TestCard> cards = new HashMap<>();

    public TestCardRepository(){
        // Tarjeta A - Saldo suficiente
        cards.put("4111111111111111", TestCard.builder()
                    .cardNumber("4111111111111111")
                    .brand("VISA")
                    .cardType("DEBIT")
                    .balance(BigDecimal.valueOf(10000))
                    .status("ACTIVE")
                    .pin("1234")
                    .build());

        // Tarjeta B - Saldo insuficiente
        cards.put("4000000000000002", TestCard.builder()
                .cardNumber("4000000000000002")
                .brand("VISA")
                .cardType("DEBIT")
                .status("ACTIVE")
                .pin("1234")
                .build());

        //Tarjeta C - Bloqueada
        cards.put("5500000000000003", TestCard.builder()
                .cardNumber("5500000000000003")
                .brand("MASTERCARD")
                .cardType("CREDIT")
                .balance(BigDecimal.valueOf(5000))
                .status("BLOCKED")
                .pin("1234")
                .build());

        //Tarjeta D - PIN incorrecto
        cards.put("3400000000000004", TestCard.builder()
                .cardNumber("3400000000000004")
                .brand("AMEX")
                .cardType("CREDIT")
                .balance(BigDecimal.valueOf(3000))
                .status("ACTIVE")
                .pin("0000")
                .build());   
                
        // Tarjeta E - Tarjeta de crédito con saldo suficiente
        cards.put("5555555555554444", TestCard.builder()
                .cardNumber("5555555555554444")
                .brand("MASTERCARD")
                .cardType("CREDIT")
                .balance(BigDecimal.valueOf(15000))
                .status("ACTIVE")
                .pin("1234")
                .build());

        log.info("{} tarjetas de prueba cargadas", cards.size());
    }
    
    //Encontrar el número de tarjeta
    public TestCard findByCardNumber(String cardNumber){
        return cards.get(cardNumber);
    }

    //Revisar si existe una tarjeta
    public boolean exists(String cardNumber){
        return cards.containsKey(cardNumber); //
    }

    //Método para obtener todas las tarjetas (útil para frontend)
    public Map<String, TestCard> getAllCards(){
        return new HashMap<>(cards);
    }
}

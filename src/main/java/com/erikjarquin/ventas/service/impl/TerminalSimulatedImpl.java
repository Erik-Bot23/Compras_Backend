package com.erikjarquin.ventas.service.impl;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.config.TerminalConfig;
import com.erikjarquin.ventas.model.dto.Terminal.TerminalRequest;
import com.erikjarquin.ventas.model.dto.Terminal.TerminalResponse;
import com.erikjarquin.ventas.model.dto.Terminal.TestCard;
import com.erikjarquin.ventas.repository.TestCardRepository;
import com.erikjarquin.ventas.service.TerminalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Terminal de pagos SIMULADA (modo por defecto, ideal para desarrollo/demo).
 *
 * <p>Se activa con {@code payment.terminal.type=SIMULATED} (matchIfMissing=true).
 * Dos modos de simulación ({@code payment.terminal.simulation.mode}):
 *  - DETERMINISTIC: usa tarjetas de prueba fijas de TestCardRepository
 *    (VISA ok, VISA sin saldo, bloqueada, PIN incorrecto, MC crédito).
 *  - RANDOM: aprueba el ~90% de las transacciones con errores aleatorios.
 *
 * <p>Nunca realiza cobros reales: es seguro para pruebas y demostraciones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.terminal.type", havingValue = "SIMULATED", matchIfMissing = true)
public class TerminalSimulatedImpl implements TerminalService {
    private final TerminalConfig config;
    private final TestCardRepository cardRepository;
    private final Random random = new Random();

    //Modo de simulación: "DETERMINISTIC" o "RANDOM"
    @Value("${payment.terminal.simulation.mode:DETERMINISTIC}")
    private String simulationMode;
    
    @Override
    public TerminalResponse processPayment(TerminalRequest request){
        log.info("[Terminal simulada] Procesando pago para venta ID: {}", request.getSaleId());
        log.info("Monto: ${}, Modo: {}", request.getAmount(), simulationMode);

        //Simular tiempo de procesamiento (con timeout)
        try{
            int delay = 500 + random.nextInt(1000);

            if(delay > config.getTimeout() * 1000){
                delay = config.getTimeout() * 1000 / 2;
            }

            Thread.sleep(delay);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }

        //Si el modo es DETERMINISTIC, user tarejtas ficticias
        if("DETERMINISTIC".equalsIgnoreCase(simulationMode)){
            return processWithTestCard(request);
        } else {
            //Modo RANDOM (aleatorio) - comportamiento original
            return processRandom(request);
        }
    }

    /*Procesamiento determista con tarjetas ficticias*/
    private TerminalResponse processWithTestCard(TerminalRequest request){
        log.info("=== PROCESANDO CON TARJETA DE PRUEBA ===");
        log.info("CardNumber recibido: '{}'", request.getCardNumber());
        log.info("PIN recibido: '{}'", request.getPin());
        log.info("Monto: {}", request.getAmount());
        log.info("TransactionId: {}", request.getTransactionId());
        
        //Usar el número de tarjeta del request si existe
        String cardNumber = request.getCardNumber();

        if(cardNumber != null){
            cardNumber = cardNumber.replaceAll("\\s+", ""); //Eliminar todos los espacios
        }

        if(cardNumber == null || cardNumber.isEmpty()){
            log.warn("No se proporcionó número de tarjeta");

            return TerminalResponse.builder()
                    .approved(false)
                    .responseCode("099")
                    .responseMessage("TARJETA NO PROPORCIONADA")
                    .errorMessage("Debe proporcionar un número de tarjeta")
                    .transactionDate(LocalDateTime.now())
                    .transactionId(request.getTransactionId())
                    .build();
        }

        if(cardNumber == null || cardNumber.isEmpty()){
            cardNumber = getCardNumberFromTransaction(request.getTransactionId());
        }

        TestCard card = cardRepository.findByCardNumber(cardNumber);

        if(card == null){
            log.warn("Tarjeta no encontrada: {}", cardNumber);
            return TerminalResponse.builder()
                                    .approved(false)
                                    .responseCode("099")
                                    .responseMessage("TARJETA NO REGISTRADA")
                                    .errorMessage("Tarjeta no encontrada en el sistema de pruebas")
                                    .transactionDate(LocalDateTime.now())
                                    .transactionId(request.getTransactionId())
                                    .build();
        }

        log.info("Tarjeta seleccionada: {} - Status: {}, Balance: ${}", card.getCardNumber(), card.getStatus(), card.getBalance());

        //Validar estado de la tarjeta
        if("BLOCKED".equalsIgnoreCase(card.getStatus())){
            log.warn("Tarjeta bloqueada");
            return TerminalResponse.builder()
                                    .approved(false)
                                    .responseCode("041")
                                    .responseMessage("TARJETA BLOQUEADA")
                                    .errorMessage("La tarjeta ha sido bloqueada por el banco")
                                    .transactionDate(LocalDateTime.now())
                                    .transactionId(request.getTransactionId())
                                    .build();
        }

        //Validar PIN correctamente
        if(request.getPin() != null && !request.getPin().equals(card.getPin())){
            log.warn("PIN incorrecto para tarjeta: {}", card.getCardNumber());
            return TerminalResponse.builder()
                                    .approved(false)
                                    .responseCode("055")
                                    .responseMessage("PIN INCORRECTO")
                                    .errorMessage("El PIN ingresado no es válido")
                                    .transactionDate(LocalDateTime.now())
                                    .transactionId(request.getTransactionId())
                                    .build();
        }

        //Validar saldo insuficiente
        if(card.getBalance().compareTo(request.getAmount()) < 0){
            log.warn("Saldo insuficiente: Disponible ${}, Necesario ${}", card.getBalance(), request.getAmount());
            return TerminalResponse.builder()
                                    .approved(false)
                                    .responseCode("051")
                                    .responseMessage("SALDO INSUFICIENTE")
                                    .errorMessage("No hay suficiente saldo en la cuenta")
                                    .transactionDate(LocalDateTime.now())
                                    .transactionId(request.getTransactionId())
                                    .build();
        }

        //Simular comunicación con el banco (solo para darle realismo)
        if(!simulateBankCommunication()){
            return TerminalResponse.builder()
                                    .approved(false)
                                    .responseCode("098")
                                    .responseMessage("ERROR DE COMUNICACIÓN")
                                    .errorMessage("No se pudo conectar con el banco emisor")
                                    .transactionDate(LocalDateTime.now())
                                    .transactionId(request.getTransactionId())
                                    .build();
        }

        //Todo ok - Transacción aprobada
        //Descontar saldo de la tarjeta (solo simulación)
        card.setBalance(card.getBalance().subtract(request.getAmount()));
        log.info("Transacción aprobada. Nuevo saldo: ${}", card.getBalance());

        return TerminalResponse.builder()
                                .approved(true)
                                .authorizationCode(generateAuthorizationCode())
                                .lastFourDigits(getLastFourDigits(card.getCardNumber()))
                                .cardBrand(card.getBrand())
                                .cardType(card.getCardType())
                                .responseCode("000")
                                .responseMessage("APROBADA")
                                .transactionDate(LocalDateTime.now())
                                .transactionId(request.getTransactionId())
                                .build();
    }

    /*Procesamiento aleatorio (modo RANDOM) - comportamiento original*/
    private TerminalResponse processRandom(TerminalRequest request){
        boolean approved = random.nextDouble() < 0.90;

        if(approved){
            return TerminalResponse.builder()
                    .approved(true)
                    .authorizationCode("AUT" + System.currentTimeMillis())
                    .lastFourDigits(String.format("%04d", random.nextInt(10000)))
                    .cardBrand(random.nextBoolean() ? "VISA" : "MASTERCARD")
                    .cardType(request.getPaymentMethod())
                    .responseCode("000")
                    .responseMessage("APROBADA")
                    .transactionDate(LocalDateTime.now())
                    .transactionId(request.getTransactionId())
                    .build();
        } else {
            String[] errors = {
                "FONDOS INSUFICIENTES",
                "TARJETA BLOQUEADA",
                "LÍMITE EXCEDIDO",
                "ERROR DE COMUNICACIÓN"
            };

            String error = errors[random.nextInt(errors.length)];

            return TerminalResponse.builder()
                    .approved(false)
                    .responseCode(random.nextBoolean() ? "051" : "061")
                    .responseMessage(error)
                    .errorMessage(error)
                    .transactionDate(LocalDateTime.now())
                    .transactionId(request.getTransactionId())
                    .build();
        }
    }

    /*Genera un número de tarjeta ficticio basado en el transactionId
     *Así aseguramos que cada transacción use una tarjeta específica de forma determinista 
    */
    private String getCardNumberFromTransaction(String transactionId){
        //Lista de tarjetas disponibles
        String[] cardNumbers = {
            "4111111111111111", // Tarjeta A (saldo suficiente)
            "4000000000000002", // Tarjeta B (saldo insuficiente)
            "5500000000000003", // Tarjeta C (bloqueada)
            "3400000000000004"  // Tarjeta D (PIN incorrecto)
        };

        //Usar el hash del transactionId para seleccionar una tarjeta
        int index = Math.abs(transactionId.hashCode() % cardNumbers.length);
        return cardNumbers[index];
    }

    //Simula la comunicación con el banco
    private boolean simulateBankCommunication(){
        //Simulación: 98% de comunicación
        return random.nextDouble() < 0.98;
    }

    //Genera código de autorización
    private String generateAuthorizationCode(){
        return "AUT" + System.currentTimeMillis() + String.format("%04d", random.nextInt(10000));
    }

    //Obtiene los últimos 4 dígitos de la tarjerta
    private String getLastFourDigits(String cardNumber){
        if(cardNumber == null || cardNumber.length() < 4){
            return "****";
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }

    //Regresar el pago
    @Override
    public boolean reversePayment(String transactionId){
        log.info("Reversando transacción: {}", transactionId);
        
        //Simulación: siempre exitosa
        return true;
    }

    //Estatus de la transacción
    @Override
    public TerminalResponse getTransactionStatus(String transactionId){
        log.info("Consultando estado de la transacción: {}", transactionId);

        //Simulación: Siempre retorna aprobado
        return TerminalResponse.builder()
                                .approved(true)
                                .transactionId(transactionId)
                                .responseCode("000")
                                .responseMessage("TRANSACCIÓN APROBADA")
                                .transactionDate(LocalDateTime.now())
                                .build();
    }
}

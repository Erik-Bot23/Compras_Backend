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
public class TerminalRequest {
    private Long saleId;
    private BigDecimal amount;
    private String paymentMethod;
    private String merchantId;
    private String terminalId;
    private String transactionId;
    private String pin;
    private String cardNumber;
}

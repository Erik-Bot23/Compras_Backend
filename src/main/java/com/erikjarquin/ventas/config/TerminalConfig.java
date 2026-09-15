package com.erikjarquin.ventas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuración de la terminal de pagos (datos de conexión y credenciales).
 *
 * <p>Se alimenta del bloque {@code payment.terminal.*} de application.yaml,
 * que a su vez se resuelve desde variables de entorno:
 *
 * <pre>
 *   PAYMENT_TERMINAL_TYPE   → SIMULATED | PHYSICAL
 *   TERMINAL_HOST / PORT    → IP y puerto de la terminal
 *   PAYMENT_MERCHANT_ID     → ID del comercio
 *   PAYMENT_TERMINAL_ID     → ID de la terminal
 *   PAYMENT_KEYSTORE_PASSWORD → contraseña del keystore (certificados SSL)
 * </pre>
 *
 * <p>SEGURIDAD: las credenciales nunca deben quedar hardcodeadas aquí ni en
 * application.yaml; viajan en variables de entorno (Railway) o en
 * application-local.yaml (dev, ignorado por git).
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment.terminal")
public class TerminalConfig {
    /** Modo de operación: "PHYSICAL" (socket real) o "SIMULATED" (pruebas). */
    private String type;
    /** IP de la terminal física. */
    private String host;
    /** Puerto TCP de la terminal física. */
    private int port;
    /** ID del comercio (merchant). */
    private String merchantId;
    /** ID de la terminal. */
    private String terminalId;
    /** Tiempo de espera máximo por operación (segundos). */
    private int timeout;
    /** Si la conexión usa SSL/TLS. */
    private boolean sslEnabled;
    /** Ruta del keystore con los certificados. */
    private String keystorePath;
    /** Contraseña del keystore. */
    private String keystorePassword;
}

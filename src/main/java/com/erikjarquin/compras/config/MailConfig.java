package com.erikjarquin.compras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuración para el envío de correos electrónicos.
 * 
 * Se alimenta del bloque {@code app.mail.*} de la application.yaml, que a su vez
 * se resuelve con las variables de entorno:
 *  MAIL_PROVIDER: smtp y resed (qué transporte se crea)
 *  RESEND_URL: url del servicio de reenvío de correos https://api.resend.com
 *  RESEND_API_KEY: api key del servicio de reenvío de correos
 *  RESEND_FROM: correo desde el que se envían los correos
 * 
 */

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailConfig {
    // Proveedor de correo electrónico a utilizar (smtp o resend)
    private String provider;

    // URL del servicio de reenvío de correos
    private String apiUrl;

    // API key del servicio de reenvío de correos
    private String apiKey;

    // Correo desde el que se envían los correos
    private String from;
}

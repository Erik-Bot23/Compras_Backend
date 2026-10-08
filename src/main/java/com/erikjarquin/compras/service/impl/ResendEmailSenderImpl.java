package com.erikjarquin.compras.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.erikjarquin.compras.config.MailConfig;
import com.erikjarquin.compras.exceptions.EmailDeliveryException;
import com.erikjarquin.compras.model.dto.Resend.ResendEmailRequest;
import com.erikjarquin.compras.model.dto.Resend.ResendEmailResponse;
import com.erikjarquin.compras.service.EmailSender;

/**
 * Envío del correo de recuperación por la API HTTPS de resend
 * 
 * Se activa con {@code app.mail.provider=resend} en application.yaml
 * 
 * Esta es la manera correcta porque Railway bloque el SMTp saliente(puertos 25/465/587).
 * Una API HTTPS sale por el puerto 443 que sí esta permitido. 
 * 
 * EL JSON se manda igual: to es una lista no un texto.
 * 
 * No usa @Transactional ni toca la BD: es una llamda HTTP
 */

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailSenderImpl implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailSenderImpl.class);

    private final MailConfig mailConfig;
    private final RestClient restClient;

    public ResendEmailSenderImpl(MailConfig mailConfig){
        this.mailConfig = mailConfig;

        /**
         * El cliente se crea una vez. La cabecera Authorization NO va aquí a
         * propósito: se agrega por petición, después de validar la clave, para
         * que el error de configuración sea un mensaje claro y no un 401.
         */
        this.restClient = RestClient.builder().baseUrl(mailConfig.getApiUrl()).build();
    }

    @Override
    public void sendPasswordRecoveryEmail(String recipient, String link){
        String apiKey = mailConfig.getApiKey();
        String from = mailConfig.getFrom();

        /**
         * Se valida antes de llamar: un 401 o un 422 de la API son el síntoma de una mala configuración y es mejor decirlo con nombre
         */
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException("Falta RESEND_API_KEY. Revisa las variables de entorno del despliegue.");
        }

        if (from == null || from.isBlank()) {
            throw new EmailDeliveryException("Falta RESEND_FROM (el remitente). Ejemplo: no-reply@tudominio.com");
        }

        ResendEmailRequest request = new ResendEmailRequest(from, List.of(recipient), "Recuperación de contraseña", "Da clic en el siguiente enlace: \n\n" + link);

        try {
            ResendEmailResponse response = restClient.post()
                                            .uri("/emails")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .header("Authorization", "Bearer " + apiKey)
                                            .body(request)
                                            .retrieve()
                                            .body(ResendEmailResponse.class);

        log.info("Correo de recuperación enviado por Resend. id={}, destinatario={}", (response != null ? response.getId() : "sin-id"), recipient);
        } catch(RestClientResponseException e){
            /**
             * La API respondió con 4xx/5xx: clave inválida, dominio no
             * verificado, límite alcanzado, etc. El cuerpo trae el detalle
             */
            throw new EmailDeliveryException("Resend rechazó el envío (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(), e);
        } catch(EmailDeliveryException e){
            throw e; //Ya es un mensaje útil, no lo reenvuelvas
        } catch(Exception e){
            //Red, DNS, timeout: no hubo respuesta de la API
            throw new EmailDeliveryException("No se pudo contactar la API de Resend: " + e.getMessage(), e);
        }


    }
}

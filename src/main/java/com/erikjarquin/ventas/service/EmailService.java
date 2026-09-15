package com.erikjarquin.ventas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos electrónicos (Gmail SMTP) para la recuperación de contraseña.
 *
 * <p>Las credenciales SMTP (MAIL_USERNAME / MAIL_PASSWORD) vienen del
 * application.yaml resuelto por variables de entorno; el remitente se toma de
 * la misma cuenta configurada para evitar dejar correos hardcodeados en el código.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Envía el enlace de recuperación de contraseña al destinatario.
     *
     * @param recipient correo del usuario que la solicitó
     * @param link      URL del frontend con el token, p. ej.
     *                  https://front/reset-password?token=xxxx (frontend-url).
     */
    public void sendPasswordRecoveryEmail(String recipient, String link) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Recuperación de contraseña");
        message.setText("Da clic en el siguiente enlace:\n\n" + link);
        mailSender.send(message);
    }
}
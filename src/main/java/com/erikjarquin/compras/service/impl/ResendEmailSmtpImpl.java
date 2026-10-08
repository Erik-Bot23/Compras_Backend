package com.erikjarquin.compras.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.erikjarquin.compras.exceptions.EmailDeliveryException;
import com.erikjarquin.compras.service.EmailSender;

/**
 * Envío de correo de recuperación de contraseña mediante SMTP (Gmail).
 * 
 * Se activa con {@code app.mail.provider=smtp} (matchIfMissing=true), o sea que es
 * el comportamiento por defecto.
 * 
 * Las credenciales SMTP (MAIL_USERNAME / MAIL_PASSWORD) vienen del
 * application.yaml resuelto por variables de entorno; el remitente se toma de 
 * la misma cuenta configurada para evitar dejar correos hardcodeados en el código.
 * 
 * En Railway Hobby NO FUNCIONA: el plan bloquea los puertos SMTP salientes
 * (25/465/587). Para producción usar {@code app.mail.provider=resend}, que
 * sale por HTTPS (443). Ver AGENTS.md 2026-10-07.
 */

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class ResendEmailSmtpImpl implements EmailSender {
    private final JavaMailSender mailSender;  
    private final String from;

    /**
     * El default vacío en @Value es a propósito: sin él, si MAIL_USERNAME 
     * no está en el entorno de Spring no resulve el placeholder y la aplicación no
     * arranca, aunque el proveedor sea resend y esta clase no se use.
     */

    public ResendEmailSmtpImpl(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from){
        this.mailSender = mailSender;
        this.from = from;
    }
    
    /**
     * Envía el enlace de recuperación de contraseña al destinatiorio.
     * 
     * @param recipient correo del usuario que la solicitó
     * @param link URL del frontend con el token, p. ej. https://front/reset-password?token=xxxx (frontend-url).
     */

    @Override
    public void sendPasswordRecoveryEmail(String recipient, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Recuperación de contraseña");
        message.setText("Da clic en el siguiente enlace:\n\n" + link);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            //Se traduce a EmailDeliveryException para que ambos proveedores (SMTP y SendGrid) tengan la misma excepción de error
            throw new EmailDeliveryException("No se pudo enviar el correo de recuperación de contraseña a " + recipient + ". por favor, inténtalo de nuevo más tarde.", e); 
        }
    }
}

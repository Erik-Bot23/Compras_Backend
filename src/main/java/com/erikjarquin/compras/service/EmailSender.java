package com.erikjarquin.compras.service;

/**
 * @author Erik Jarquin
 * @version 1.0
 * @since 2024-06-10
 * Contrato/interfaz para el envío de correos electrónicos.
 * EmailService -> SMTP(app.mail.provider=smtp, valor por defecto)
 * ResendEmailSender -> HTTPS(app.mail.provider=resend)
 * SMTP y API HTTPS son dos transportes distintos (puertos 587 vc 443). Quié llama a AuthImpl
 * no debe saber cuál está activo, solo hay qu enviar el correo. Esto ayuda a probar con MOCk
 * 
 * EmailService esta ocupado por la clase concreta de SMTP
 */
public interface EmailSender {
    /**Envía el enlace de recuperación de contraseña al destinatario */

    void sendPasswordRecoveryEmail(String recipient, String link);
}

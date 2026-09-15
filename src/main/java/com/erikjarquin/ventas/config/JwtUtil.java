package com.erikjarquin.ventas.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidades para generar y validar tokens JWT (JSON Web Token).
 *
 * <p>El token es HS256 y su "subject" es el email del usuario.
 * La expiración es de 5 horas desde su emisión.
 *
 * <p>IMPORTANTE (seguridad): el secreto de firma NO está hardcodeado.
 * Se inyecta desde la variable de entorno ${JWT_SECRET}, que en local
 * vive en application-local.yaml y en Railway en las variables del servicio.
 * Un secreto filtrado a Git permitiría a cualquiera forjar tokens de ADMIN.
 */
@Component
public class JwtUtil {

    /**
     * Secreto de firma HS256 (≥ 32 bytes). Resuelto en runtime desde:
     *  - Variable de entorno JWT_SECRET (Railway) o
     *  - application-local.yaml (dev, ignorado por git).
     * Cualquiera puede ver el valor si este archivo se sube con el secreto,
     * por eso nunca debe quedar escrito aquí como literal.
     */
    @Value("${JWT_SECRET}")
    private String secretKey;

    /**
     * Genera un token firmado HS256 con subject = email del usuario.
     * Lleva fecha de emisión y expiración (5 horas).
     */
    public String generateToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 5))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrae el email (subject) del token. Lanza si el token es inválido,
     * inválido o expirado.
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Parsea el token y devuelve sus claims. Lanza JwtException si la firma
     * no coincide, el token está expirado o está malformado.
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Valida si el token es válido (firma correcta y no expirado).
     * Cualquier error de firma/expiracion/formato devuelve {@code false}.
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
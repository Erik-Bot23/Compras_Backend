package com.erikjarquin.ventas.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración central de seguridad.
 *
 * <p>Modelo: API stateless con JWT. CSRF desactivado (no hay cookies de sesión),
 * CORS limitado a los orígenes permitidos y autorización por PERMISO
 * mediante {@code @PreAuthorize} en cada controller.
 *
 * <p>Los endpoints públicos son: {@code OPTIONS /**} (preflight), {@code /ping}
 * (healthcheck), {@code /api/auth/**} (login/recuperación) y
 * {@code /api/uploads/**} (imágenes de productos).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Orígenes CORS permitidos (separados por coma). Se resuelve desde:
     *  - ${CORS_ALLOWED_ORIGINS} (Railway o application-local.yaml)
     *  - default: http://localhost:4200 (desarrollo).
     * En producción Netlify deberá ser: https://tu-app.netlify.app
     */
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Encripta contraseñas con BCrypt antes de guardarlas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura qué orígenes pueden llamar a la API desde un navegador.
     * Los orígenes se indican en la propiedad {@code CORS_ALLOWED_ORIGINS} como
     * lista separada por comas: "http://localhost:4200,https://app.netlify.app".
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(splitCorsOrigins(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * Divide la cadena de orígenes CORS y limpia espacios/valores vacíos.
     *
     * @param raw "http://localhost:4200, https://otro.com"
     * @return lista con cada origen recortado y sin entradas vacías.
     */
    private List<String> splitCorsOrigins(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * Cadena de filtros de Spring Security:
     *  1. CSRF desactivado (API stateless con JWT).
     *  2. CORS habilitado con la fuente configurada arriba.
     *  3. Rutas públicas: OPTIONS (preflight), /ping, /api/auth/**, /api/uploads/**.
     *  4. Cualquier otra ruta requiere autenticación (JWT).
     *  5. JwtFilter se ejecuta antes del filtro de usuario/contraseña estándar.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/ping").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
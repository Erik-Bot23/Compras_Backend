package com.erikjarquin.ventas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Healthcheck simple de la aplicación.
 *
 * <p>No lleva prefijo {@code /api}. Está marcado como PÚBLICO en SecurityConfig
 * (rutas {@code /ping} → permitAll), así Railway/balanceadores pueden comprobar
 * liveness o vendría siendo un "¿está viva la app?" sin necesidad de JWT.
 */
@RestController
public class pingController {

    /**
     * Devuelve una respuesta mínima 200 OK. Descripción: revisar el estado.
     */
    @GetMapping("/ping")
    public String ping() {
        return "Ok";
    }
}
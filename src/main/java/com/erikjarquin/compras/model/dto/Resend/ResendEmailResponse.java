package com.erikjarquin.compras.model.dto.Resend;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de la API de Resend (POST /emails)
 * Tras el identificador del envío, que sirve para registrarlo en el log y depurar problemas de entrega. 
 * La API de Resend no devuelve el estado de entrega del correo, solo un identificador.
 */
public class ResendEmailResponse {
    @JsonProperty("id")
    private String id;

    public ResendEmailResponse() {}

    public ResendEmailResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

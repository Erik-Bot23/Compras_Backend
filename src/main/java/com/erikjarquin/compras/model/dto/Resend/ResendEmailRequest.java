package com.erikjarquin.compras.model.dto.Resend;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo del JSON que espera la API de Resend (POST /emails)
 * 
 * to es una lista. Si se mandará como texto, la API respondería 422
 * y el correo no se enviaría. Por eso se manda como lista, aunque solo tenga un elemento.
 * 
 * Jackson ya viene con spring-boot-starter-web, por lo que no es necesario agregarlo como dependencia.
 */
public class ResendEmailRequest {
    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private List<String> to;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("text")
    private String text;

    public ResendEmailRequest(){}

    public ResendEmailRequest(String from, List<String> to, String subject, String text) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.text = text;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

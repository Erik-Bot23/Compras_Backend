package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;

/**
 * Petición de apertura de caja ({@code POST /api/local/cash/open}, V3).
 *
 * <p><b>number</b> es la caja existente que se va a abrir. Antes este endpoint
 * creaba la caja y por eso el número se escribía aquí; ahora la caja se crea
 * aparte ({@code POST /api/local/cash}) y aquí solo se elige cuál abrir. Ver
 * {@link CreateCashRequest} para el porqué del cambio.
 *
 * <p><b>openingAmount</b> es el fondo con el que arranca el turno. Tiene un
 * mínimo de 100 porque una caja que abre con 0 o con 50 no permite ni una venta
 * pequeña y además esconde los errores de captura: escribir "0" casi siempre
 * significa que se olvidó el campo, no que el cajón esté vacío.
 */
public class OpenCashRequest {

    /**
     * Fondo inicial del turno. Obligatorio, no negativo y <b>mínimo 100</b>
     * (regla del dueño, 2026-09-30).
     */
    private BigDecimal openingAmount;

    /**
     * Número de la caja que se abre (V3). Debe existir y no haber sido usada
     * antes: una caja es un turno, y reabrir la misma mezclaría dos cortes.
     */
    private String number;

    public OpenCashRequest(){}

    //Getter y setter de openingAmount
    public BigDecimal getOpeningAmount(){
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount){
        this.openingAmount = openingAmount;
    }

    //Getter y setter de number
    public String getNumber(){
        return number;
    }

    public void setNumber(String number){
        this.number = number;
    }
}

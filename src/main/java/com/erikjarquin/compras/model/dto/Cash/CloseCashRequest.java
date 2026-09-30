package com.erikjarquin.compras.model.dto.Cash;

import java.math.BigDecimal;

/**
 * Petición de CIERRE de caja ({@code POST /api/local/cash/close}).
 *
 * <p><b>closingAmount</b> es el efectivo que el cajero.contó en el cajón. El
 * backend lo compara contra el "esperado" (fondo inicial + ventas en efectivo) y
 * calcula la diferencia.
 *
 * <p><b>differenceReason</b> (V3, 2026-09-30) es la <i>salida de emergencia</i>
 * del cuadre. Si la diferencia es distinta de cero, el cierre se rechaza con
 * <b>409</b> y este campo queda vacío: no se puede cerrar un turno con el dinero
 * descuadrado sin explicar por qué. Si el cajero está seguro de que el monto es
 * correcto (se equivocó al contar, o hubo una entrega mal hecha), envía el motivo
 * y el cierre proceeds, dejando la diferencia y su causa registradas.
 *
 * <p>La diferencia y su motivo van juntos a propósito: "me sobraron 200" y "me
 * faltaron 200" son problemas opuestos con el mismo síntoma, y un reporte que
 * solo dice "hubo descuadre" no ayuda a resolver ninguno de los dos.
 */
public class CloseCashRequest {

    /** Efectivo contado por el cajero. Obligatorio y no negativo. */
    private BigDecimal closingAmount;

    /**
     * Motivo del descuadre. <b>Obligatorio solo si hay diferencia.</b> Si el
     * efectivo contado coincide exactamente con el esperado, el backend lo ignora
     * (y lo limpia) para que no queden motivos huérfanos en un corte que cuadró.
     */
    private String differenceReason;

    public CloseCashRequest(){}

    //Getter y setter de closingAmount
    public BigDecimal getClosingAmount(){
        return closingAmount;
    }

    public void setClosingAmount(BigDecimal closingAmount){
        this.closingAmount=closingAmount;
    }

    //Getter y setter de differenceReason
    public String getDifferenceReason(){
        return differenceReason;
    }

    public void setDifferenceReason(String differenceReason){
        this.differenceReason=differenceReason;
    }
}

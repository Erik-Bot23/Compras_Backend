package com.erikjarquin.compras.model.dto.Cash;

/**
 * Petición de CREACIÓN de una caja física ({@code POST /api/local/cash}, V3).
 *
 * <p>Es un concepto nuevo respecto a antes: la caja ya no nace al abrirla, sino
 * que se <b>registra</b> primero y después se abre una de las existentes. Igual
 * que una caja real de un negocio, que se rotula antes de usarse.
 *
 * <p>El motivo de separarlo es que el número tiene que existir <i>antes</i> de
 * abrir, para poder elegirlo de una lista. Antes el número se escribía en el
 * momento de abrir, cuando ya no había nada que elegir.
 *
 * <p>El número es <b>UNIQUE</b> en la base de datos porque identifica un corte
 * sin ambigüedad: sin eso, dos cortes distintos compartirían número y el reporte
 * "filtrar por caja" mezclaría dos turnos en una misma fila.
 */
public class CreateCashRequest {

    /** Número de la caja, p. ej. "CAJA 1". Obligatorio. */
    private String number;

    public CreateCashRequest(){}

    public String getNumber(){
        return number;
    }

    public void setNumber(String number){
        this.number=number;
    }
}

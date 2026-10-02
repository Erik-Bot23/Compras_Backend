package com.erikjarquin.compras.model.entity;

public class CashBoxEntity {
    /**
     Entidad cash_boxes: el registro de las cajas fisicas del local
     Por qué existe separada de CashRegisterEntity. Hasta V3 no habia
     ninguna: cada fila de cash_registers era "una caja" y por eso esa
     fila se cerraba una sola vez, para siempre. Pero un negocio real tiene dos o tres
     cajas que se abren y cierran todas los días.
     */

     /**
      * Las cajas fisicas. Pocas filas, cambian poco, es la lista
        que el cajero elige al abrir
      */
}

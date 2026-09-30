package com.erikjarquin.compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erikjarquin.compras.model.entity.CashRegisterEntity;

/**
 * Repositorio de cajas.
 *
 * <p>{@code findByActiveTrue} devuelve la caja vigente; la existencia de UNA caja
 * abierta es la invariante del módulo (la regla la aplica {@code CashRegisterImpl},
 * no la base de datos).
 *
 * <p>Desde V3 la caja se CREA antes de abrirse, así que conviven tres estados que
 * se distinguen por columnas y no por el enum de la fila:
 * <ul>
 *   <li><b>Creada sin abrir</b>: {@code openedAt == null}, {@code active == false}.</li>
 *   <li><b>Abierta</b>: {@code openedAt != null}, {@code closedAt == null},
 *       {@code active == true}.</li>
 *   <li><b>Cerrada</b>: {@code closedAt != null}, {@code active == false}.</li>
 * </ul>
 */
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, Long> {

    //La caja vigente (una sola a la vez, por regla de negocio)
    Optional<CashRegisterEntity> findByActiveTrue();

    /**
     * ¿Existe alguna caja con este número? Se usa al CREAR una caja para devolver
     * un 409 con mensaje, en vez de un 500 por violar el UNIQUE de la base de
     * datos.
     */
    boolean existsByNumber(String number);

    //Busca por número exacto. El servicio normaliza antes (recorta y capitaliza)
    Optional<CashRegisterEntity> findByNumber(String number);

    /**
     * Historial completo (V3), la más reciente primero. Incluye las cajas que se
     * crearon pero nunca se abrieron, que por eso tienen {@code openedAt == null}
     * y salen primero: son las más nuevas.
     */
    List<CashRegisterEntity> findAllByOrderByOpenedAtDesc();

    /**
     * Cajas NUNCA abiertas (V3): las candidatas para abrir.
     *
     * <p>El criterio es {@code opened_at IS NULL}, no {@code active = false}. Una
     * caja recién creada no tiene {@code openedAt} y una caja ya cerrada lo tiene:
     * así se distingue "todavía no se usó" de "ya teve su turno", que es
     * justamente lo que impide reabrir la misma caja dos veces y mezclar dos
     * turnos en un mismo corte.
     */
    List<CashRegisterEntity> findByOpenedAtIsNullOrderByNumberAsc();
}

package com.erikjarquin.compras.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erikjarquin.compras.model.entity.PurchaseEntity;

/**
 * Repositorio de compras (cabecera).
 *
 * <p>findAll/findByProvider/findDetailedById usan {@code @EntityGraph} para
 * precargar provider + details.product en UNA consulta (evita N+1 en los
 * listados) y así el mapper puede leer product.name sin volver a tocar la BD.
 */
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {

    @EntityGraph(attributePaths = {"provider", "details.product"})
    List<PurchaseEntity> findAllByOrderByPurchaseDateDesc();

    //Compras de un proveedor concreto (listado por proveedor)
    @EntityGraph(attributePaths = {"provider", "details.product"})
    List<PurchaseEntity> findByProviderIdOrderByPurchaseDateDesc(Long providerId);

    //Compra completa (GET /{id}: detalles + producto + proveedor)
    @EntityGraph(attributePaths = {"provider", "details.product"})
    Optional<PurchaseEntity> findDetailedById(Long id);

    //¿Cuántas compras tiene un proveedor? (para bloquear su borrado con 409)
    long countByProvider_Id(Long providerId);

    /**
     * Marca la compra como CONFIRMADA de forma <b>atómica y condicional</b> (V3).
     *
     * <p>Devuelve el número de filas afectadas, y eso es lo importante:
     * <ul>
     *   <li><b>1</b> → esta petición ganó la carrera: puede aplicar el stock.</li>
     *   <li><b>0</b> → ya estaba confirmada, así que otra petición se adelantó.
     *       Quien pierde NO debe tocar el stock.</li>
     * </ul>
     *
     * <h3>Por qué esto y no un simple {@code if}</h3>
     *
     * <p>Un {@code if (entity.isConfirmed())} lee la fila y decide en Java. Con
     * dos peticiones simultáneas, <b>las dos leen {@code false}</b> antes de que
     * ninguna escriba, y las dos suman el stock: la compra termina con el doble
     * de mercancía.
     *
     * <p>Esta variante lo resuelve en UNA sentencia de SQL. El {@code WHERE
     * confirmed = false} hace que la segunda transacción espere al lock de fila
     * de la primera y, al reevaluar la condición, ya no se cumpla y devuelva
     * 0. La condición y la escritura son atómicas juntas, que es justo lo que el
     * {@code if} no era.
     *
     * <p>Se prefirió esto a {@code @Lock(PESSIMISTIC_WRITE)} porque no mantiene
     * un lock de fila abierto durante todo el {@code @Transactional} (incluidos
     * los {@code save()} de cada producto del renglón) y porque no depende de que
     * nadie se acuerde de anotar el método de lectura.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PurchaseEntity p
        SET p.confirmed = true,
            p.confirmedAt = :when
        WHERE p.id = :id AND p.confirmed = false
    """)
    int markConfirmedIfPending(@Param("id") Long id, @Param("when") LocalDateTime when);
}
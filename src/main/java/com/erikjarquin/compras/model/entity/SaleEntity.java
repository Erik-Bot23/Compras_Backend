package com.erikjarquin.compras.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.erikjarquin.compras.model.enums.PaymentMethod;
import com.erikjarquin.compras.model.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entidad {@code sales}: cabecera de una venta.
 *
 * <p>Relaciones: 1:N a SaleDetailEntity (productos vendidos, cascade ALL +
 * orphanRemoval), N:1 a CashRegisterEntity (caja donde se registró) y 1:1 a
 * PaymentEntity (solo si fue pagada con tarjeta). paymentStatus recorre:
 * PENDING → APPROVED | REJECTED | REVERSED.
 *
 * <h2>Ciclo de vida de la venta (agregado 2026-09-30)</h2>
 *
 * <p>Una venta tiene DOS ejes de estado INDEPENDIENTES y no se mezclan:
 *
 * <table border="1">
 *   <caption>Los dos ejes</caption>
 *   <tr><th>Eje</th><th>Campos</th><th>Pregunta que responde</th></tr>
 *   <tr><td>Pago</td><td>{@code paymentStatus}</td>
 *       <td>¿Se cobró el dinero? (PENDING/APPROVED/REJECTED/REVERSED)</td></tr>
 *   <tr><td>Venta</td><td>{@code confirmed}, {@code cancelled}</td>
 *       <td>¿El cliente ya se llevó el pedido? ¿Se anuló?</td></tr>
 * </table>
 *
 * <p>Son ejes separados porque una venta con tarjeta APROBADA puede estar
 * CANCELADA: se cobró y después se devolvió el dinero. Meter un estado
 * "CANCELLED" dentro de {@link PaymentStatus} habría mezclado "estado del pago"
 * con "estado de la venta" y habría hecho touched también al CHECK
 * {@code payments_status_check} de la BD.
 *
 * <p><b>Reglas (las aplica SaleImpl, no la entidad):</b>
 * <ul>
 *   <li>Venta recien creada: {@code confirmed=false}, {@code cancelled=false}
 *       → se puede anular (error de caja).</li>
 *   <li>Confirmada: {@code confirmed=true} → CONGELADA. Ya no se anula ni se
 *       borra jamás, porque el stock y el dinero son reales.</li>
 *   <li>Anulada: {@code cancelled=true} → el stock volvió al inventario, pero
 *       la fila se CONSERVA (auditoría: no se borran ventas).</li>
 *   <li>Nunca ambas a la vez: son mutuamente excluyentes.</li>
 * </ul>
 *
 * <p>Por qué {@code boolean} y no un {@code enum}: un enum obligaría a que las
 * filas ya existentes migraran de un valor a otro y a que el estado "no
 * decidido" fuera un valor más. Con dos banderas el estado inicial es el valor
 * natural (false/false) y {@code DEFAULT false} deja las ventas históricas en
 * "abierta", que es lo correcto: se pueden confirmar después.
 */
@Entity
@Table(name = "sales")
public class SaleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime saleDate;

    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal cashReceived;

    private BigDecimal changeAmount;

    @OneToMany(
        mappedBy = "sale",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<SaleDetailEntity> details;

    @ManyToOne
    @JoinColumn(name = "cash_register_id")
    private CashRegisterEntity cashRegister;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentEntity payment;

    // ===== Ciclo de vida de la venta (ver V2__venta_confirmada.sql) =====
    // `nullable = false` en el mapeo NO es lo mismo que un NOT NULL en BD: la
    // BD ya lo garantiza (V2), JPA solo lo documenta. El `columnDefinition` fija
    // el DEFAULT para que un INSERT que no mencione la columna (p. ej. un
    // test que arma la entidad a mano) guarde false y no null.

    /** true = venta congelada: no se puede anular ni borrar. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean confirmed;

    /** Momento en que se confirmó; null mientras la venta siga abierta. */
    private LocalDateTime confirmedAt;

    /** true = venta anulada (el stock volvió al inventario). */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean cancelled;

    /** Momento de la anulación; null si la venta no fue anulada. */
    private LocalDateTime cancelledAt;

    public SaleEntity(){}

    //Getter y setter de id
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    //Getter y setter de saleDate
    public LocalDateTime getSaleDate(){
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate){
        this.saleDate=saleDate;
    }

    //Getter y setter de total
    public BigDecimal getTotal(){
        return total;
    }

    public void setTotal(BigDecimal total){
        this.total=total;
    }

    //Getter y setter de paymentMethod
    public PaymentMethod getPaymentMethod(){
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod){
        this.paymentMethod=paymentMethod;
    }

    //Getter y setter de cashReceived
    public BigDecimal getCashReceived(){
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived){
        this.cashReceived=cashReceived;
    }

    //Getter y setter de changeAmount
    public BigDecimal getChangeAmount(){
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount){
        this.changeAmount=changeAmount;
    }

    //Getter y setter de details
    public List<SaleDetailEntity> getDetails(){
        return details;
    }

    public void setDetails(List<SaleDetailEntity> details){
        this.details=details;
    }

    //Getter y setter de CashRegister
    public CashRegisterEntity getCashRegister(){
        return cashRegister;
    }

    public void setCashRegister(CashRegisterEntity cashRegister){
        this.cashRegister=cashRegister;
    }

    //Getter y setter de paymentStatus
    public PaymentStatus getPaymentStatus(){
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus){
        this.paymentStatus=paymentStatus;
    }

    //Getter y setter de payment
    public PaymentEntity getPayment(){
        return payment;
    }

    public void setPayment(PaymentEntity payment){
        this.payment = payment;
    }

    //===== Ciclo de vida: getters y setters =====
    //En una entidad JPA, `boolean` (no Boolean) evita el,null-trap: un Boolean
    //sin asignar es null y un `if (confirmado)` sobre un Boolean desboxea null
    //y lanza NullPointerException. Con boolean primitivo el valor por defecto
    //del objeto Java es false y no hay sorpresas.
    public boolean isConfirmed(){
        return confirmed;
    }

    public void setConfirmed(boolean confirmed){
        this.confirmed = confirmed;
    }

    public LocalDateTime getConfirmedAt(){
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt){
        this.confirmedAt = confirmedAt;
    }

    public boolean isCancelled(){
        return cancelled;
    }

    public void setCancelled(boolean cancelled){
        this.cancelled = cancelled;
    }

    public LocalDateTime getCancelledAt(){
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt){
        this.cancelledAt = cancelledAt;
    }

}

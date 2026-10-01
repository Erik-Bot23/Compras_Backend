package com.erikjarquin.compras.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.compras.exceptions.CashException;
import com.erikjarquin.compras.mapper.CashRegisterMapper;
import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.CreateCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.compras.model.entity.CashRegisterEntity;
import com.erikjarquin.compras.model.entity.SaleEntity;
import com.erikjarquin.compras.repository.CashRegisterRepository;
import com.erikjarquin.compras.repository.SaleRepository;
import com.erikjarquin.compras.service.CashRegisterService;

/**
 * Implementación de la caja registradora.
 *
 * El modelo cambió en V3 (2026-09-30): la caja se CREA antes de abrirse.
 * Antes, {@code POST /open} creaba la fila y el número se escribía en ese
 * momento. Ahora hay dos pasos:
 * 
 * {@link #create(CreateCashRequest)}: se registra la caja física con su
 *       número. La fila nace con {@code openedAt = null}.
 * {@link #open(OpenCashRequest)}: se elige una de las cajas ya registradas
 *       y se abre. El número tiene que existir antes para poder elegirlo.
 * 
 *
 * Consecuencia: una caja se abre UNA sola vez. El número es UNIQUE, o
 * sea una fila por caja física, así que reabrir "CAJA 1" metería dos turnos en
 * el mismo corte y el reporte por caja mostraría ventas de la mañana junto a
 * ventas de la tarde como si fueran del mismo turno. Para un turno nuevo se crea
 * "CAJA 2".
 *
 * Reglas que se siguen respetando:
 * 
 * Una sola caja abierta a la vez (409 si ya hay una).
 * El fondo inicial mínimo es 100 y nunca negativo.
 * El cierre exige cuadrar el efectivo, con salida de emergencia que
 *       pide un motivo.
 * 
 */
@Service
public class CashRegisterImpl implements CashRegisterService {

    /**
     * Fondo mínimo con el que se puede abrir una caja (decisión del dueño).
     *
     * Además de sentido practical (una caja con 0 no alcanza ni para una
     * venta), evita que un forgot del campo se guarde como 0 y/contamine el
     * "esperado" del corte sin que nadie lo note.
     */
    private static final BigDecimal MIN_OPENING_AMOUNT = new BigDecimal("100");

    /** Longitud máxima del número, igual que el VARCHAR(50) de la columna. */
    private static final int MAX_NUMBER_LENGTH = 50;

    /**
     * Límite del motivo del descuadre, igual que el VARCHAR(255) de la columna.
     * Si se pasa, la BD lo truncaría en silencio y se perdería la mitad de la
     * explicación.
     */
    private static final int MAX_REASON_LENGTH = 255;

    private final CashRegisterRepository repository;
    private final CashRegisterMapper mapper;
    private final SaleRepository saleRepository;

    public CashRegisterImpl(
        CashRegisterRepository repository,
        CashRegisterMapper mapper,
        SaleRepository saleRepository){
        this.repository = repository;
        this.mapper = mapper;
        this.saleRepository=saleRepository;
    }

    // =========================================================================
    //  CREAR la caja física (V3)
    // =========================================================================

    /**
     * Registra una caja nueva, todavía sin abrir.
     *
     * La fila nace con {@code openedAt = null} y {@code active = false}, que
     * es lo que la distingue de una caja ya cerrada (esa tiene ambos
     * informados). En V1 {@code opened_at} y {@code opening_amount} ya eran
     * nullable, así que no hizo falta alterar la tabla para soportar cajas que
     * todavía no abren.
     */
    @Override
    @Transactional
    public CashResponse create(CreateCashRequest request){
        String number = normalizeNumber(request.getNumber());

        //Se valida aquí y no solo con el UNIQUE de la BD: la base lanzaría una
        //violación de constraint (500, sin mensaje útil); esto da un 409 que
        //explica por qué importa que sea único.
        if(repository.existsByNumber(number)){
            throw new CashException(
                "Ya existe una caja con el numero \"" + number + "\". "
                + "Cada caja necesita un numero unico para poder filtrar los reportes.",
                HttpStatus.CONFLICT);
        }

        CashRegisterEntity cash = new CashRegisterEntity();
        cash.setNumber(number);
        //Sin esto la columna NOT NULL de total_tickets (V1) revienta al insertar.
        cash.setTotalTickets(0);
        cash.setActive(false);
        //openedAt y openingAmount se quedan en null: esta caja aun no abre.

        return mapper.toResponse(repository.save(cash));
    }

    /**
     * Cajas todavía sin abrir: las que el usuario puede elegir para abrir.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CashResponse> getAvailable(){
        return repository.findByOpenedAtIsNullOrderByNumberAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Sugiere el siguiente número libre con el patrón "CAJA n" (V3).
     *
     * Existe para que el modal de crear caja venga con "CAJA 7" ya escrito en
     * vez de obligar a contar cuántas hay. Solo sugiere: el usuario puede
     * cambiarlo, y si elSuggested número ya existe, {@link #create} lo rechaza
     * con 409 en vez de fallar en silencio.
     */
    @Override
    @Transactional(readOnly = true)
    public String getNextSuggestedNumber(){
        int max = 0;

        for(CashRegisterEntity cash : repository.findAll()){
            String number = cash.getNumber();
            if(number == null){
                continue;
            }
            //Solo interessan los que siguen el patrón "CAJA <número>"
            String[] parts = number.trim().split("\\s+");
            if(parts.length != 2 || !parts[0].equalsIgnoreCase("CAJA")){
                continue;
            }
            try{
                max = Math.max(max, Integer.parseInt(parts[1]));
            }catch(NumberFormatException ignored){
                //Un número escrito a mano ("CAJA PRINCIPAL") no cuenta para el
                //siguiente: no es un error, solo no sigue el patrón.
            }
        }

        return "CAJA " + (max + 1);
    }

    // =========================================================================
    //  ABRIR y CERRAR
    // =========================================================================

    /**
     * Abre una caja YA REGISTRADA (V3).
     *
     * Tres validaciones, en este orden y por razones distintas:
     * 
     *   Solo una caja abierta a la vez: si ya hay una activa, 409.
     *       Es la regla de siempre, no cambia.
     *   La caja debe existir: 404 si el número no está registrado. No
     *       se crea al vuelo como antes, porque el número tiene que poder elegirse.
     *   La caja no puede haberse usado antes: 409 si {@code openedAt}
     *       ya está informado, porque una caja es un turno.
     * 
     */
    @Override
    @Transactional
    public CashResponse open(OpenCashRequest request){
        repository.findByActiveTrue().ifPresent(c -> {
            throw new CashException(
                "Ya existe una caja abierta (" + c.getNumber()
                + "). Ciérrala antes de abrir otra.", HttpStatus.CONFLICT);
        });

        String number = normalizeNumber(request.getNumber());

        CashRegisterEntity cash = repository.findByNumber(number)
                .orElseThrow(() -> new CashException(
                    "No existe una caja registrada con el numero \"" + number
                    + "\". Crerala primero con el botón \"Crear caja\".",
                    HttpStatus.NOT_FOUND));

        //Una caja ya abierta o ya cerrada no se reutiliza (ver el javadoc de la
        //clase): reabrirla mezclaría dos turnos en el mismo corte.
        if(cash.getOpenedAt() != null){
            throw new CashException(
                "La caja \"" + number + "\" ya se usó"
                + (cash.getClosedAt() != null ? " y se cerró el " + cash.getClosedAt() : "")
                + ". Una caja es un turno: crea una caja nueva para el siguiente.",
                HttpStatus.CONFLICT);
        }

        BigDecimal openingAmount = validateOpeningAmount(request.getOpeningAmount());

        //Inicializa los totales en 0 para que la caja exista desde el arranque
        //aunque no tenga ventas, y el corte muestre ceros en vez de nulos.
        cash.setCashSales(BigDecimal.ZERO);
        cash.setDebitSales(BigDecimal.ZERO);
        cash.setCreditSales(BigDecimal.ZERO);
        cash.setTotalSales(BigDecimal.ZERO);
        cash.setExpectedAmount(openingAmount);
        cash.setDifference(BigDecimal.ZERO);
        cash.setDifferenceReason(null);

        cash.setOpenedAt(LocalDateTime.now());
        cash.setOpeningAmount(openingAmount);
        cash.setActive(true);

        return mapper.toResponse(repository.save(cash));
    }

    /**
     * Valida el fondo inicial: obligatorio, no negativo y mínimo 100.
     *
     * Se hace aquí y no con {@code @Valid} en el DTO porque el mensaje puede
     * decir qué límite se rompió y por qué, que es más útil que "validation
     * failed".
     */
    private BigDecimal validateOpeningAmount(BigDecimal openingAmount){
        if(openingAmount == null){
            throw new CashException(
                "El monto inicial de la caja es obligatorio", HttpStatus.BAD_REQUEST);
        }
        if(openingAmount.signum() < 0){
            throw new CashException(
                "El monto inicial no puede ser negativo", HttpStatus.BAD_REQUEST);
        }
        if(openingAmount.compareTo(MIN_OPENING_AMOUNT) < 0){
            throw new CashException(
                "El monto inicial debe ser al menos $" + MIN_OPENING_AMOUNT.toPlainString()
                + ". Una caja no puede abrir con menos.",
                HttpStatus.BAD_REQUEST);
        }
        return openingAmount;
    }

    /**
     * Cierra la caja exigiendo que el efectivo contado cuadre.
     *
     * Por qué bloquea y por qué tiene salida
     *
     * Si el efectivo contado no coincide con el esperado, el cierre se rechaza
     * con 409. La razón de fondo: el dinero que falta en el cajón es dinero
     * que salió del negocio, y un corte que "cierra igual" convierte un robo en
     * un descuadre invisible.
     *
     * Pero bloquear sin salida tiene un costo real: si el cajero se equivocó al
     * contar, o un cliente entregó un billete falso y le dio mal el cambio, el
     * turno quedaría encerrado y no se podría cerrar hasta que alguien investigue.
     * Por eso existe {@code differenceReason}: si el cajero está seguro del monto
     * que cuenta, escribe por qué no cuadra y el cierre proceeds. La diferencia y
     * su motivo quedan guardados juntos, y con eso el descuadre deja de ser
     * invisible.
     */
    @Override
    @Transactional
    public CashResponse close(CloseCashRequest request){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() ->
            new CashException("No existe caja abierta", HttpStatus.NOT_FOUND));

        CashSummaryResponse summary = calculateSummary(cash);

        if(request.getClosingAmount() == null){
            throw new CashException(
                "Debes indicar cuánto efectivo hay en la caja", HttpStatus.BAD_REQUEST);
        }
        if(request.getClosingAmount().signum() < 0){
            throw new CashException(
                "El efectivo contado no puede ser negativo", HttpStatus.BAD_REQUEST);
        }

        BigDecimal countedAmount = request.getClosingAmount();
        BigDecimal difference = countedAmount.subtract(summary.getExpectedAmount());

        // ===== EL CUADRE =====
        //Con diferencia 0 el motivo se ignora y se limpia: un corte que cuadró no
        //debe quedar con un motivo de descuadre colgado de una versión anterior.
        if(difference.signum() != 0){
            String reason = normalizeReason(request.getDifferenceReason());

            if(reason == null){
                //409 y NO se escribe nada: la caja sigue abierta.
                throw new CashException(
                    "El dinero no cuadra. Esperado: $" + summary.getExpectedAmount().toPlainString()
                    + ", contado: $" + countedAmount.toPlainString()
                    + ", diferencia: " + signed(difference)
                    + ". Revisa el conteo. Si estás seguro de que el monto es correcto, "
                    + "cierra con la salida de emergencia escribiendo el motivo.",
                    HttpStatus.CONFLICT);
            }
            cash.setDifferenceReason(reason);
        }else{
            cash.setDifferenceReason(null);
        }

        // ===== Guardar el corte =====
        cash.setCountedAmount(countedAmount);
        cash.setCashSales(summary.getCashSales());
        cash.setDebitSales(summary.getDebitSales());
        cash.setCreditSales(summary.getCreditSales());
        cash.setTotalSales(summary.getTotalSales());
        cash.setExpectedAmount(summary.getExpectedAmount());
        cash.setDifference(difference);
        cash.setTotalTickets(summary.getTotalTickets());
        cash.setClosedAt(LocalDateTime.now());
        cash.setActive(false);

        return mapper.toResponse(repository.save(cash));
    }

    /**
     * Motivo del descuadre: obligatorio si se avisó, y acotado al VARCHAR(255).
     * Devuelve null si no se envió nada, que es lo que dispara el 409.
     */
    private String normalizeReason(String reason){
        if(reason == null || reason.isBlank()){
            return null;
        }

        String trimmed = reason.trim();

        if(trimmed.length() > MAX_REASON_LENGTH){
            //Se recorta en vez de dejar que la BD lo trunque en silencio: mejor un
            //motivo corto y legible que uno cortado a la mitad sin avisar.
            return trimmed.substring(0, MAX_REASON_LENGTH);
        }

        return trimmed;
    }

    /** Formatea la diferencia con signo explícito (+50 / -50), que se lee mejor. */
    private String signed(BigDecimal difference){
        return (difference.signum() > 0 ? "+" : "") + difference.toPlainString();
    }

    /**
     * Normaliza el número de caja: recorta y <b>capitaliza</b>.
     *
     * Se capitaliza a propósito: "caja 1" y "CAJA 1" son la MISMA caja y
     * deben ser el mismo número. Sin esto el UNIQUE de PostgreSQL los dejaría
     * pasar como dos cajas distintas (compara las mayúsculas y minúsculas tal
     * cual) y el reporte de esa caja mesclaría dos cortes reales.
     */
    private String normalizeNumber(String number){
        if(number == null || number.isBlank()){
            throw new CashException("El numero de caja es obligatorio", HttpStatus.BAD_REQUEST);
        }

        String normalized = number.trim().toUpperCase();

        if(normalized.length() > MAX_NUMBER_LENGTH){
            throw new CashException(
                "El numero de caja no puede tener mas de " + MAX_NUMBER_LENGTH + " caracteres",
                HttpStatus.BAD_REQUEST);
        }

        return normalized;
    }

    // =========================================================================
    //  Consultas
    // =========================================================================

    //Ver la caja activa
    @Override
    @Transactional(readOnly = true)
    public CashResponse getActiveCash(){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() ->
            new CashException("No existe caja abierta"));

        return mapper.toResponse(cash);
    }

    /**
     * Historial de cajas (V3): la más reciente primero, para elegir cuál filtrar.
     * Incluye las que se crearon pero todavía no se abrieron.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CashResponse> getHistory(){
        return repository.findAllByOrderByOpenedAtDesc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    //Detalle de una caja concreta por su número (V3)
    @Override
    @Transactional(readOnly = true)
    public CashResponse getByNumber(String number){
        return repository.findByNumber(normalizeNumber(number))
                .map(mapper::toResponse)
                .orElseThrow(() -> new CashException(
                    "No existe ninguna caja con el numero \"" + number + "\"", HttpStatus.NOT_FOUND));
    }

    //Ver el resumen de la venta
    @Override
    @Transactional(readOnly = true)
    public CashSummaryResponse getSummary(){
        CashRegisterEntity cash = repository.findByActiveTrue().orElseThrow(() ->
            new CashException("No existe caja abierta"));

        return calculateSummary(cash);
    }

    /**
     * Calcula el resumen del corte: totales por método de pago y el efectivo
     * esperado (fondo inicial + ventas en efectivo).
     *
     * Las ventas ANULADAS se saltan: anular una venta en efectivo no devuelve
     * el dinero al cajón, así que su monto ya no está ahí. Contarlas dejaría su
     * dinero dentro del "esperado" y el cajero vería una diferencia fantasma de
     * su propio bolsillo. Mismo criterio que el filtro
     * {@code cancelled = false} de los reportes de Ventas.
     */
    private CashSummaryResponse calculateSummary(CashRegisterEntity cash){
        List<SaleEntity> sales = saleRepository.findByCashRegister(cash);

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal debitSales = BigDecimal.ZERO;
        BigDecimal creditSales = BigDecimal.ZERO;

        for(SaleEntity sale : sales){
            if(sale.isCancelled()){
                continue;
            }

            switch(sale.getPaymentMethod()){
                case CASH -> cashSales = cashSales.add(sale.getTotal());

                case DEBIT -> debitSales = debitSales.add(sale.getTotal());

                case CREDIT -> creditSales = creditSales.add(sale.getTotal());
            }
        }

        BigDecimal totalSales = cashSales.add(debitSales).add(creditSales);
        BigDecimal opening = cash.getOpeningAmount() == null ? BigDecimal.ZERO : cash.getOpeningAmount();
        BigDecimal expectedAmount = opening.add(cashSales);

        CashSummaryResponse response = new CashSummaryResponse();
        response.setCashId(cash.getId());
        response.setOpeningAmount(opening);
        response.setCashSales(cashSales);
        response.setDebitSales(debitSales);
        response.setCreditSales(creditSales);
        response.setTotalSales(totalSales);
        response.setExpectedAmount(expectedAmount);
        //Ticketes contados aparte porque las anuladas no suman dinero pero sí
        //fueron tickets: si se contaran, el cajero vería más tickets que ventas.
        response.setTotalTickets(countValidTickets(sales));

        return response;
    }

    private int countValidTickets(List<SaleEntity> sales){
        int total = 0;
        for(SaleEntity sale : sales){
            if(!sale.isCancelled()){
                total++;
            }
        }
        return total;
    }
}

package com.erikjarquin.compras.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.erikjarquin.compras.exceptions.CashException;
import com.erikjarquin.compras.mapper.CashRegisterMapper;
import com.erikjarquin.compras.model.dto.Cash.CashResponse;
import com.erikjarquin.compras.model.dto.Cash.CloseCashRequest;
import com.erikjarquin.compras.model.dto.Cash.CreateCashRequest;
import com.erikjarquin.compras.model.dto.Cash.OpenCashRequest;
import com.erikjarquin.compras.model.entity.CashRegisterEntity;
import com.erikjarquin.compras.model.enums.PaymentMethod;
import com.erikjarquin.compras.model.enums.PaymentStatus;
import com.erikjarquin.compras.model.entity.SaleEntity;
import com.erikjarquin.compras.repository.CashRegisterRepository;
import com.erikjarquin.compras.repository.SaleRepository;

/**
 * Tests de la caja V3: crear, abrir, y el cuadre del efectivo al cerrar.
 *
 * <p>El bloque de {@link CuadreTest} es el que documenta la regla del dueño: el
 * cierre exige que el dinero cuadre, pero hay una salida de emergencia que pide
 * un motivo. Los dos comportamientos se prueban por separado porque son
 * decisiones distintas y una anula a la otra.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Caja V3 - crear, abrir y cuadrar")
class CashRegisterV3Test {

    @Mock private CashRegisterRepository repository;
    @Mock private CashRegisterMapper mapper;
    @Mock private SaleRepository saleRepository;

    @InjectMocks private CashRegisterImpl service;

    //Helper: caja recién creada (sin abrir)
    private CashRegisterEntity cajaSinAbrir(String number){
        CashRegisterEntity cash = new CashRegisterEntity();
        cash.setNumber(number);
        cash.setActive(false);
        cash.setTotalTickets(0);
        return cash;
    }

    //Helper: caja abierta
    private CashRegisterEntity cajaAbierta(String number, BigDecimal fondo){
        CashRegisterEntity cash = cajaSinAbrir(number);
        cash.setActive(true);
        cash.setOpenedAt(LocalDateTime.now());
        cash.setOpeningAmount(fondo);
        return cash;
    }

    // =========================================================================
    //  CREAR
    // =========================================================================
    @Nested
    @DisplayName("Crear la caja física")
    class CrearTest {

        @Test
        @DisplayName("crea la caja sin abrirla (openedAt queda en null)")
        void creaSinAbrir(){
            CreateCashRequest request = new CreateCashRequest();
            request.setNumber("CAJA 1");

            when(repository.existsByNumber("CAJA 1")).thenReturn(false);
            when(repository.save(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> {
                        CashRegisterEntity c = i.getArgument(0);
                        CashResponse r = new CashResponse();
                        r.setNumber(c.getNumber());
                        r.setActive(c.getActive());
                        return r;
                    });

            CashResponse r = service.create(request);

            assertThat(r.getNumber()).isEqualTo("CAJA 1");
            assertThat(r.getActive()).isFalse();

            verify(repository).save(any(CashRegisterEntity.class));
        }

        @Test
        @DisplayName("número repetido -> 409, no un 500 por violar el UNIQUE")
        void numeroRepetidoDa409(){
            CreateCashRequest request = new CreateCashRequest();
            request.setNumber("CAJA 1");

            when(repository.existsByNumber("CAJA 1")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("Ya existe")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any(CashRegisterEntity.class));
        }

        @Test
        @DisplayName("normaliza a mayúsculas: 'caja 1' y 'CAJA 1' son la misma")
        void normalizaMayusculas(){
            CreateCashRequest request = new CreateCashRequest();
            request.setNumber("  caja 1  ");

            when(repository.existsByNumber("CAJA 1")).thenReturn(false);
            when(repository.save(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> new CashResponse());

            service.create(request);

            verify(repository).existsByNumber("CAJA 1");
        }

        @Test
        @DisplayName("número vacío -> 400")
        void numeroVacioDa400(){
            CreateCashRequest request = new CreateCashRequest();
            request.setNumber("  ");

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(CashException.class)
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("sugiere el siguiente número libre: CAJA 1 cuando no hay ninguna")
        void sugiereSiguienteNumero(){
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.getNextSuggestedNumber()).isEqualTo("CAJA 1");
        }

        @Test
        @DisplayName("sugiere el siguiente ignorando las que no siguen el patrón")
        void sugiereIgnoraNombresLibres(){
            CashRegisterEntity a = cajaSinAbrir("CAJA 1");
            CashRegisterEntity b = cajaSinAbrir("CAJA PRINCIPAL"); //no numérica
            when(repository.findAll()).thenReturn(List.of(a, b));

            assertThat(service.getNextSuggestedNumber()).isEqualTo("CAJA 2");
        }
    }

    // =========================================================================
    //  ABRIR
    // =========================================================================
    @Nested
    @DisplayName("Abrir la caja")
    class AbrirTest {

        private OpenCashRequest request(String numero, String fondo){
            OpenCashRequest r = new OpenCashRequest();
            r.setNumber(numero);
            r.setOpeningAmount(new BigDecimal(fondo));
            return r;
        }

        @Test
        @DisplayName("abre una caja registrada y le pone la fecha")
        void abreCajaRegistrada(){
            CashRegisterEntity cash = cajaSinAbrir("CAJA 1");
            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 1")).thenReturn(Optional.of(cash));
            when(repository.save(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> new CashResponse());

            service.open(request("CAJA 1", "500"));

            assertThat(cash.getOpenedAt()).isNotNull();
            assertThat(cash.getActive()).isTrue();
            assertThat(cash.getOpeningAmount()).isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("caja inexistente -> 404 con la pista de crearla primero")
        void cajaInexistenteDa404(){
            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 9")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.open(request("CAJA 9", "500")))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("Crerala primero")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("una caja ya usada NO se puede volver a abrir (mezclaría 2 turnos)")
        void cajaYaUsadaNoSeReabre(){
            CashRegisterEntity cash = cajaSinAbrir("CAJA 1");
            cash.setOpenedAt(LocalDateTime.now().minusHours(5));
            cash.setClosedAt(LocalDateTime.now().minusHours(1));

            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 1")).thenReturn(Optional.of(cash));

            assertThatThrownBy(() -> service.open(request("CAJA 1", "500")))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("ya se usó")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any(CashRegisterEntity.class));
        }

        @Test
        @DisplayName("si ya hay una caja abierta -> 409 y menciona cuál es")
        void yaHayUnaAbiertaDa409(){
            when(repository.findByActiveTrue())
                    .thenReturn(Optional.of(cajaAbierta("CAJA 1", new BigDecimal("500"))));

            assertThatThrownBy(() -> service.open(request("CAJA 2", "500")))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("CAJA 1")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("fondo menor a 100 -> 400 (regla del dueño)")
        void fondoMenorACienDa400(){
            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 1")).thenReturn(Optional.of(cajaSinAbrir("CAJA 1")));

            assertThatThrownBy(() -> service.open(request("CAJA 1", "50")))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("al menos")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("fondo negativo -> 400")
        void fondoNegativoDa400(){
            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 1")).thenReturn(Optional.of(cajaSinAbrir("CAJA 1")));

            assertThatThrownBy(() -> service.open(request("CAJA 1", "-500")))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("negativo")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("fondo exactamente 100 sí se acepta (es el mínimo, no el tope)")
        void fondoEnElMinimoSeAcepta(){
            CashRegisterEntity cash = cajaSinAbrir("CAJA 1");
            when(repository.findByActiveTrue()).thenReturn(Optional.empty());
            when(repository.findByNumber("CAJA 1")).thenReturn(Optional.of(cash));
            when(repository.save(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class)))
                    .thenAnswer(i -> new CashResponse());

            service.open(request("CAJA 1", "100"));

            assertThat(cash.getOpeningAmount()).isEqualByComparingTo("100");
        }
    }

    // =========================================================================
    //  EL CUADRE AL CERRAR
    // =========================================================================
    @Nested
    @DisplayName("Cuadre del efectivo al cerrar")
    class CuadreTest {

        //Una venta en efectivo de 200
        private List<SaleEntity> ventaEnEfectivoDe(BigDecimal total){
            SaleEntity sale = new SaleEntity();
            sale.setTotal(total);
            sale.setPaymentMethod(PaymentMethod.CASH);
            sale.setPaymentStatus(PaymentStatus.APPROVED);
            sale.setCancelled(false);
            sale.setSaleDate(LocalDateTime.now());
            return List.of(sale);
        }

        private CloseCashRequest request(String contado, String motivo){
            CloseCashRequest r = new CloseCashRequest();
            r.setClosingAmount(new BigDecimal(contado));
            r.setDifferenceReason(motivo);
            return r;
        }

        @Test
        @DisplayName("si cuadra exactamente, cierra y NO guarda motivo")
        void cierraSiCuadra(){
            //fondo 500 + venta 200 en efectivo = esperado 700
            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(ventaEnEfectivoDe(new BigDecimal("200")));
            when(repository.save(any(CashRegisterEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class))).thenAnswer(i -> new CashResponse());

            service.close(request("700", null));

            assertThat(cash.getDifference()).isEqualByComparingTo("0");
            assertThat(cash.getDifferenceReason()).isNull();
            assertThat(cash.getActive()).isFalse();
        }

        @Test
        @DisplayName("NO cuadra y sin motivo -> 409 y la caja SIGUE ABIERTA")
        void noCuadraSinMotivoBloquea(){
            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(ventaEnEfectivoDe(new BigDecimal("200")));

            assertThatThrownBy(() -> service.close(request("650", null)))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("no cuadra")
                    .hasMessageContaining("700")  //el esperado, para que compare
                    .hasMessageContaining("650")  //el contado
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            //Lo importante: NO se guardó nada, la caja sigue abierta
            verify(repository, never()).save(any(CashRegisterEntity.class));
            assertThat(cash.getActive()).isTrue();
        }

        @Test
        @DisplayName("NO cuadra pero con motivo -> cierra y guarda la diferencia y el motivo")
        void noCuadraConMotivoCierra(){
            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(ventaEnEfectivoDe(new BigDecimal("200")));
            when(repository.save(any(CashRegisterEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class))).thenAnswer(i -> new CashResponse());

            service.close(request("650", "Se mePaso un billete de 50 al contar"));

            assertThat(cash.getDifference()).isEqualByComparingTo("-50");
            assertThat(cash.getDifferenceReason()).isEqualTo("Se mePaso un billete de 50 al contar");
            assertThat(cash.getActive()).isFalse();
        }

        @Test
        @DisplayName("motivo en blanco cuenta como no enviado: también bloquea")
        void motivoEnBlancoBloquea(){
            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(ventaEnEfectivoDe(new BigDecimal("200")));

            assertThatThrownBy(() -> service.close(request("650", "   ")))
                    .isInstanceOf(CashException.class)
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("efectivo contado negativo -> 400")
        void efectivoNegativoDa400(){
            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(ventaEnEfectivoDe(new BigDecimal("200")));

            assertThatThrownBy(() -> service.close(request("-100", null)))
                    .isInstanceOf(CashException.class)
                    .hasMessageContaining("negativo")
                    .extracting(e -> ((CashException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        /**
         * Las ventas anuladas no cuentan ni para el dinero ni para los tickets.
         *
         * <p>Contarlas dejaría el monto de una venta anulada dentro del "esperado":
         * el cajero contaría el efectivo real, no coincidiría, y vería una
         * diferencia fantasma de su propio bolsillo.
         */
        @Test
        @DisplayName("las ventas anuladas no entran al cálculo del corte")
        void ventasAnuladasNoCuentan(){
            SaleEntity anulada = new SaleEntity();
            anulada.setTotal(new BigDecimal("200"));
            anulada.setPaymentMethod(PaymentMethod.CASH);
            anulada.setPaymentStatus(PaymentStatus.APPROVED);
            anulada.setCancelled(true);
            anulada.setSaleDate(LocalDateTime.now());

            CashRegisterEntity cash = cajaAbierta("CAJA 1", new BigDecimal("500"));
            when(repository.findByActiveTrue()).thenReturn(Optional.of(cash));
            when(saleRepository.findByCashRegister(cash)).thenReturn(List.of(anulada));
            when(repository.save(any(CashRegisterEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any(CashRegisterEntity.class))).thenAnswer(i -> new CashResponse());

            //Si la anulada contara, el esperado sería 700. Como no cuenta, es 500.
            service.close(request("500", null));

            assertThat(cash.getExpectedAmount()).isEqualByComparingTo("500");
            assertThat(cash.getTotalTickets()).isZero();
        }
    }
}

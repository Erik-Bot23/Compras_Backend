package com.erikjarquin.compras.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.erikjarquin.compras.exceptions.PurchaseException;
import com.erikjarquin.compras.model.dto.Purchases.PurchaseDTO;
import com.erikjarquin.compras.model.dto.Purchases.PurchaseItemRequest;
import com.erikjarquin.compras.model.dto.Purchases.PurchaseRequest;
import com.erikjarquin.compras.model.entity.ProductEntity;
import com.erikjarquin.compras.model.entity.ProviderEntity;
import com.erikjarquin.compras.model.entity.PurchaseDetailEntity;
import com.erikjarquin.compras.model.entity.PurchaseEntity;
import com.erikjarquin.compras.repository.ProductRepository;
import com.erikjarquin.compras.repository.ProviderRepository;
import com.erikjarquin.compras.repository.PurchaseRepository;

/**
 * Tests unitarios de PurchaseImpl (Mockito, sin BD).
 *
 * <p>Verifican la REGLA DE NEGOCIO del módulo en su diseño V3: registrar una
 * compra <b>no</b> toca el inventario, {@code confirm} es lo que suma stock y
 * guarda el costo, y una compra confirmada ya no se puede cancelar.
 *
 * <h3>Por qué estos tests parecen "repetir" lo obvio</h3>
 *
 * <p>La tentación es pensar que "crear no suma stock" no necesita un test. Sí
 * lo necesita, y mucho: hasta V2 {@code create()} SÍ sumaba stock, y ese
 * {@code verify(productRepository, never()).save(...)} es lo que impide que
 * alguien reintroduzca el efecto "de paso" cuando agregue un campo nuevo.
 *
 * <h3>El bug del {@code Math.max}, ahora estructuralmente imposible</h3>
 *
 * <p>El bug de 2026-09-30 (comprar 10 → vender 5 → cancelar escribía un
 * {@code Math.max(5 - 10, 0)} = <b>0 falso</b> con 5 unidades ya vendidas y
 * devolvía 200 OK) se cubría con una validación de stock en dos fases. En V3
 * ese escenario ya no se puede representar: la compra pendiente nunca tocó el
 * inventario, así que no hay nada que deshacer. Los tests lo fijan desde el otro
 * lado: {@code cancelarCompra_pendiente_nuncaLeeLosProductos} exige que cancelar
 * ni siquiera <b>consulte</b> los productos, y
 * {@code confirmarCompra_pendiente_sumaStockYGuardaCosto} exige que el stock se
 * aplique una sola vez.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PurchaseImpl purchaseService;

    //Helper: request de compra mínima (1 renglón)
    private PurchaseRequest request(Long providerId, Long productId, int qty, String unitCost){
        PurchaseItemRequest item = new PurchaseItemRequest();
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setUnitCost(new BigDecimal(unitCost));

        PurchaseRequest request = new PurchaseRequest();
        request.setProviderId(providerId);
        request.setItems(List.of(item));
        return request;
    }

    //Helper: proveedor con id
    private ProviderEntity provider(Long id, String name){
        ProviderEntity provider = new ProviderEntity();
        provider.setId(id);
        provider.setName(name);
        return provider;
    }

    //Helper: producto con id
    private ProductEntity product(Long id, String name, int stock){
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        product.setStock(stock);
        return product;
    }

    // =========================================================================
    //  REGISTRAR: nace PENDIENTE y NO toca el inventario (V3)
    // =========================================================================

    @Test
    void crearCompra_nacePendienteYNoTocaElInventario(){
        ProviderEntity provider = provider(1L, "Proveedor Alfa");
        ProductEntity cafe = product(10L, "Café 1kg", 5);

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(productRepository.findById(10L)).thenReturn(Optional.of(cafe));
        when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseDTO dto = purchaseService.create(request(1L, 10L, 3, "40"));

        // Nace PENDIENTE: sin confirmar y sin fecha de confirmación
        assertThat(dto.isConfirmed()).isFalse();
        assertThat(dto.getConfirmedAt()).isNull();

        // El inventario NO se tocó: ni stock ni costo
        assertThat(cafe.getStock())
                .as("registrar una compra no debe sumar stock: eso es lo de confirmar")
                .isEqualTo(5);
        assertThat(cafe.getCost())
                .as("registrar una compra no debe escribir el costo: eso es lo de confirmar")
                .isEqualByComparingTo(BigDecimal.ZERO);

        // LO IMPORTANTE: no se escribió el producto en absoluto
        verify(productRepository, never()).save(any(ProductEntity.class));

        // El total sí se calcula al registrar (es el dato del documento)
        assertThat(dto.getTotal()).isEqualByComparingTo("120.00");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getProviderName()).isEqualTo("Proveedor Alfa");
    }

    @Test
    void crearCompra_proveedorInexistenteLanza409(){
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.create(request(99L, 10L, 2, "50")))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("proveedor");
    }

    @Test
    void crearCompra_sinItemsLanzaIllegalArgumentException(){
        PurchaseRequest request = new PurchaseRequest();
        request.setProviderId(1L);
        request.setItems(List.of());

        assertThatThrownBy(() -> purchaseService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artículo");
    }

    // =========================================================================
    //  CONFIRMAR: aquí sí entran la mercancía y el costo
    // =========================================================================

    @Test
    void confirmarCompra_pendiente_sumaStockYGuardaCosto(){
        ProductEntity cafe = product(10L, "Café 1kg", 5);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(cafe);
        detail.setQuantity(3);
        detail.setUnitCost(new BigDecimal("40"));

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(5L);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(5L)).thenReturn(Optional.of(purchase));
        when(productRepository.findById(10L)).thenReturn(Optional.of(cafe));
        when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseDTO dto = purchaseService.confirm(5L);

        // Stock + costo: el efecto por el que existe el módulo de compras
        assertThat(cafe.getStock()).isEqualTo(8);
        assertThat(cafe.getCost()).isEqualByComparingTo("40");

        // Queda CONFIRMADA y con fecha de confirmación
        assertThat(dto.isConfirmed()).isTrue();
        assertThat(dto.getConfirmedAt()).isNotNull();

        verify(productRepository).save(cafe);
        verify(purchaseRepository).save(purchase);
    }

    /**
     * Confirmar dos veces NO debe sumar el stock dos veces.
     *
     * <p>Es el test que blinda la idempotencia: el doble clic o el reintento
     * tras un corte de red son el caso normal en un POS, y "devolver el stock
     * dos veces" es exactamente el tipo de error que nadie nota hasta que el
     * inventario no cuadra.
     */
    @Test
    void confirmarCompra_yaConfirmada_esIdempotenteYNoSumaStockDosVeces(){
        ProductEntity cafe = product(10L, "Café 1kg", 8);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(cafe);
        detail.setQuantity(3);
        detail.setUnitCost(new BigDecimal("40"));

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(5L);
        purchase.setConfirmed(true);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(5L)).thenReturn(Optional.of(purchase));

        PurchaseDTO dto = purchaseService.confirm(5L);

        assertThat(dto.isConfirmed()).isTrue();
        assertThat(cafe.getStock())
                .as("una compra ya confirmada no vuelve a sumar stock")
                .isEqualTo(8);

        // Cero escrituras: ni del producto ni de la compra
        verify(productRepository, never()).save(any(ProductEntity.class));
        verify(purchaseRepository, never()).save(any(PurchaseEntity.class));
    }

    @Test
    void confirmarCompra_inexistenteLanza404(){
        when(purchaseRepository.findDetailedById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.confirm(404L))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void confirmarCompra_dosRenglones_sumaTodosLosStocks(){
        ProductEntity leche = product(10L, "Leche", 20);
        ProductEntity cafe = product(11L, "Café 1kg", 2);

        PurchaseDetailEntity detLeche = new PurchaseDetailEntity();
        detLeche.setProduct(leche);
        detLeche.setQuantity(10);
        detLeche.setUnitCost(new BigDecimal("25"));

        PurchaseDetailEntity detCafe = new PurchaseDetailEntity();
        detCafe.setProduct(cafe);
        detCafe.setQuantity(4);
        detCafe.setUnitCost(new BigDecimal("40"));

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(9L);
        purchase.setDetails(List.of(detLeche, detCafe));

        when(purchaseRepository.findDetailedById(9L)).thenReturn(Optional.of(purchase));
        when(productRepository.findById(10L)).thenReturn(Optional.of(leche));
        when(productRepository.findById(11L)).thenReturn(Optional.of(cafe));
        when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        purchaseService.confirm(9L);

        assertThat(leche.getStock()).isEqualTo(30);
        assertThat(cafe.getStock()).isEqualTo(6);
        assertThat(leche.getCost()).isEqualByComparingTo("25");
        assertThat(cafe.getCost()).isEqualByComparingTo("40");
        verify(productRepository, times(2)).save(any(ProductEntity.class));
    }

    // =========================================================================
    //  CANCELAR: solo pendientes, y sin tocar NADA
    // =========================================================================

    @Test
    void cancelarCompra_pendiente_laBorraYNoEscribe(){
        ProductEntity cafe = product(10L, "Café 1kg", 8);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(cafe);
        detail.setQuantity(3);

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(5L);
        purchase.setConfirmed(false);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(5L)).thenReturn(Optional.of(purchase));

        purchaseService.cancel(5L);

        verify(purchaseRepository).delete(purchase);

        // La garantía del diseño: una compra pendiente nunca tocó el
        // inventario, así que cancelar no consulta NI ESCRIBE los productos.
        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(ProductEntity.class));
        assertThat(cafe.getStock()).isEqualTo(8);
    }

    @Test
    void cancelarCompra_inexistenteLanza404(){
        when(purchaseRepository.findDetailedById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.cancel(404L))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("no encontrada");
    }

    // =========================================================================
    //  CASO QUE CUBRE EL BUG ORIGINAL (2026-09-30), ahora por otra vía
    // =========================================================================

    /**
     * El escenario reportado por el usuario: comprar 10, vender 5 (quedan 5) y
     * luego cancelar la compra.
     *
     * <p>El código viejo hacía {@code 5 - 10 = -5} y luego
     * {@code Math.max(-5, 0) = 0}: reportaba "0 en almacén" con 5 unidades ya
     * vendidas y devolvía 200 OK.
     *
     * <p>Hoy, con el stock en 5 y la compra en estado PENDIENTE, ese escenario
     * es imposible por construcción: una compra pendiente no suman stock. Y si
     * la compra está CONFIRMADA, se rechaza con 409. Este test fija el segundo
     * caso, que es el que el usuario podría alcanzar: no se escribe nada.
     */
    @Test
    void cancelarCompra_confirmada_Lanza409YNoEscribe(){
        // Compramos 10, se vendió 5 -> quedan 5, y la compra está confirmada.
        ProductEntity cafe = product(10L, "Café 1kg", 5);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(cafe);
        detail.setQuantity(10);

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(5L);
        purchase.setConfirmed(true);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(5L)).thenReturn(Optional.of(purchase));

        assertThatThrownBy(() -> purchaseService.cancel(5L))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("confirmada")
                .extracting(e -> ((PurchaseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // LO MÁS IMPORTANTE: ni el stock ni la fila se tocaron
        assertThat(cafe.getStock())
                .as("el stock NO debe tocarse si la operación se rechaza")
                .isEqualTo(5);

        verify(productRepository, never()).save(any(ProductEntity.class));
        verify(purchaseRepository, never()).delete(any(PurchaseEntity.class));
    }

    /**
     * Cancelar una compra confirmada da 409 <b>aunque el stock de sobra</b>.
     *
     * <p>Este es el test que mejor explica por qué el estado es lo que manda y
     * no el stock: con 50 unidades en almacén la validación antigua habría
     * permitido "deshacer" la compra y restado 10, dejando el inventario
     * inconsistente con lo que realmente llegó. El costo y el stock ya son
     * hechos; la compra es evidencia de una entrega real.
     */
    @Test
    void cancelarCompra_confirmadaConStockDeSobra_Lanza409(){
        ProductEntity pan = product(20L, "Pan", 50);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(pan);
        detail.setQuantity(10);

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(6L);
        purchase.setConfirmed(true);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(6L)).thenReturn(Optional.of(purchase));

        assertThatThrownBy(() -> purchaseService.cancel(6L))
                .isInstanceOf(PurchaseException.class)
                .extracting(e -> ((PurchaseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(pan.getStock()).isEqualTo(50);
        verify(productRepository, never()).save(any(ProductEntity.class));
        verify(purchaseRepository, never()).delete(any(PurchaseEntity.class));
    }
}

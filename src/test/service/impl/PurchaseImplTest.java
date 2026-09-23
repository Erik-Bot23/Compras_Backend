package com.erikjarquin.test.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.erikjarquin.ventas.exceptions.PurchaseException;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseDTO;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseItemRequest;
import com.erikjarquin.ventas.model.dto.Purchases.PurchaseRequest;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.entity.ProviderEntity;
import com.erikjarquin.ventas.model.entity.PurchaseDetailEntity;
import com.erikjarquin.ventas.model.entity.PurchaseEntity;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.repository.ProviderRepository;
import com.erikjarquin.ventas.repository.PurchaseRepository;
import com.erikjarquin.ventas.service.impl.PurchaseImpl;

/**
 * Tests unitarios de PurchaseImpl (Mockito, sin BD): verifican la REGLA DE
 * NEGOCIO clave del m\u00f3dulo — crear compra suma stock y guarda el costo
 * real del producto, y cancelarla revierte el stock — adem\u00e1s de las
 * validaciones de entrada.
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

    @Test
    void crearCompra_sumaStockYGuardaCostoReal(){
        ProviderEntity provider = new ProviderEntity();
        provider.setId(1L);
        provider.setName("Proveedor Alfa");

        ProductEntity producto = new ProductEntity();
        producto.setId(10L);
        producto.setName("Café 1kg");
        producto.setPrice(new BigDecimal("150"));
        producto.setStock(5);

        when(providerRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseRequest request = new PurchaseRequest();
        request.setProviderId(1L);

        PurchaseItemRequest item = new PurchaseItemRequest();
        item.setProductId(10L);
        item.setQuantity(3);
        item.setUnitCost(new BigDecimal("40"));
        request.setItems(List.of(item));

        PurchaseDTO dto = purchaseService.create(request);

        //Efecto sobre el producto: +3 unidades y costo actualizado
        assertThat(producto.getStock()).isEqualTo(8);
        assertThat(producto.getCost()).isEqualByComparingTo("40.00");

        //Total = unitCost * quantity
        assertThat(dto.getTotal()).isEqualByComparingTo("120.00");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getProviderName()).isEqualTo("Proveedor Alfa");
    }

    @Test
    void crearCompra_proveedorInexistenteLanza409(){
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        PurchaseRequest request = new PurchaseRequest();
        request.setProviderId(99L);

        PurchaseItemRequest item = new PurchaseItemRequest();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setUnitCost(new BigDecimal("50"));
        request.setItems(List.of(item));

        assertThatThrownBy(() -> purchaseService.create(request))
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

    @Test
    void cancelarCompra_revierteStockYElimina(){
        ProductEntity producto = new ProductEntity();
        producto.setId(10L);
        producto.setName("Café 1kg");
        producto.setStock(8);

        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setProduct(producto);
        detail.setQuantity(3);

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(5L);
        purchase.setDetails(List.of(detail));

        when(purchaseRepository.findDetailedById(5L)).thenReturn(Optional.of(purchase));
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));

        purchaseService.cancel(5L);

        assertThat(producto.getStock()).isEqualTo(5);
        verify(purchaseRepository).delete(purchase);
    }

    @Test
    void cancelarCompra_compraInexistenteLanza404(){
        when(purchaseRepository.findDetailedById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.cancel(404L))
                .isInstanceOf(PurchaseException.class)
                .hasMessageContaining("no encontrada");
    }
}
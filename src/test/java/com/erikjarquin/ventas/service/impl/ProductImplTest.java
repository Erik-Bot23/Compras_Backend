package com.erikjarquin.ventas.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.erikjarquin.ventas.exceptions.ProductException;
import com.erikjarquin.ventas.mapper.ProductMapper;
import com.erikjarquin.ventas.model.dto.Products.ProductDto;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.repository.CategoryRepository;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.service.FileStorageService;

/**
 * Tests unitarios de ProductImpl (Mockito, sin BD) centrados en el BORRADO
 * LÓGICO de productos: dar de baja (active=false conservando la fila/imagen),
 * reactivar, listar los dados de baja, filtrar los activos del catálogo y
 * bloquear el borrado real cuando el producto tiene ventas/compras.
 */
@ExtendWith(MockitoExtension.class)
class ProductImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    //Se construye a mano (con un ProductMapper real) para verificar también el
    //mapeo de active/hasHistory hacia el DTO.
    private ProductImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductImpl(productRepository, categoryRepository,
                fileStorageService, new ProductMapper(""));
    }

    private ProductEntity producto(Long id, boolean active) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setName("Café 1kg");
        p.setPrice(new BigDecimal("150.00"));
        p.setStock(5);
        p.setActive(active);
        return p;
    }

    @Test
    void darDeBaja_poneActiveFalseYConservaLaImagen() {
        ProductEntity producto = producto(10L, true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productRepository.existsBySaleDetailsProductId(10L)).thenReturn(true);
        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto dto = productService.deactivate(10L);

        assertThat(producto.isActive()).isFalse();
        assertThat(dto.isActive()).isFalse();
        assertThat(dto.isHasHistory()).isTrue();
        //El soft delete NO borra el archivo de imagen ni la fila.
        verify(fileStorageService, never()).delete(any());
        verify(productRepository, never()).deleteById(anyLong());
        verify(productRepository).save(producto);
    }

    @Test
    void darDeBaja_yaInactivoLanza409() {
        ProductEntity producto = producto(11L, false);
        when(productRepository.findById(11L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productService.deactivate(11L))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("ya está dado de baja");

        verify(productRepository, never()).save(any());
    }

    @Test
    void reactivar_poneActiveTrue() {
        ProductEntity producto = producto(12L, false);
        when(productRepository.findById(12L)).thenReturn(Optional.of(producto));
        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto dto = productService.activate(12L);

        assertThat(producto.isActive()).isTrue();
        assertThat(dto.isActive()).isTrue();
        verify(productRepository).save(producto);
    }

    @Test
    void listarInactivos_devuelveSoloLosDadosDeBaja() {
        when(productRepository.findByActiveFalse()).thenReturn(List.of(producto(20L, false)));

        List<ProductDto> resultado = productService.getInactive();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isActive()).isFalse();
        verify(productRepository, never()).findAll();
    }

    @Test
    void listarActivos_usaFindByActiveTrue() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(producto(30L, true)));

        List<ProductDto> resultado = productService.getAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isActive()).isTrue();
        //Ya no se usa findAll(): los dados de baja quedan fuera del catálogo.
        verify(productRepository, never()).findAll();
    }

    @Test
    void buscarPorCodigo_productoDadoDeBaja_noSeEncuentra() {
        when(productRepository.findByBarcode("7501234567890"))
                .thenReturn(Optional.of(producto(40L, false)));

        assertThatThrownBy(() -> productService.findByBarcode("7501234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void eliminar_conHistorico_lanza409YSinBorrarImagen() {
        ProductEntity producto = producto(50L, true);
        when(productRepository.findById(50L)).thenReturn(Optional.of(producto));
        when(productRepository.existsBySaleDetailsProductId(50L)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(50L))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("No se puede eliminar");

        verify(fileStorageService, never()).delete(any());
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    void eliminar_sinHistorico_borraImagenYFila() {
        ProductEntity producto = producto(51L, true);
        producto.setImg("foto.jpg");
        when(productRepository.findById(51L)).thenReturn(Optional.of(producto));

        productService.delete(51L);

        verify(fileStorageService).delete("foto.jpg");
        verify(productRepository).deleteById(51L);
    }
}

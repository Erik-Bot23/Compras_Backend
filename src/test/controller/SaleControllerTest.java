package com.erikjarquin.test.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.erikjarquin.ventas.config.JwtUtil;
import com.erikjarquin.ventas.config.SecurityConfig;
import com.erikjarquin.ventas.config.security.SecurityAuthorityMapper;
import com.erikjarquin.ventas.controller.SaleController;
import com.erikjarquin.ventas.exceptions.SaleException;
import com.erikjarquin.ventas.model.dto.Sale.SaleResponse;
import com.erikjarquin.ventas.model.enums.PaymentMethod;
import com.erikjarquin.ventas.model.enums.PaymentStatus;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.SaleService;

/**
 * Tests del controlador de ventas.
 *
 * <p>Verifica el POST de venta (200), el historial GET (lista y detalle) y que
 * un error de negocio del servicio (SaleException) se traduzca a 400 con el
 * formato uniforme.
 */
@WebMvcTest(SaleController.class)
@Import(SecurityConfig.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaleService saleService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    private SaleResponse ventaAprobada() {
        SaleResponse response = new SaleResponse();
        response.setSaleId(7L);
        response.setTotal(new BigDecimal("100.00"));
        response.setPaymentMethod(PaymentMethod.CASH);
        response.setCashReceived(new BigDecimal("120.00"));
        response.setChangeAmount(new BigDecimal("20.00"));
        response.setPaymentStatus(PaymentStatus.APPROVED);
        return response;
    }

    @Test
    @WithMockUser(authorities = "CREAR_VENTAS")
    void registrarVenta_devuelveVentaAprobada() throws Exception {
        when(saleService.processSale(any())).thenReturn(ventaAprobada());

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"cashReceived\":120,\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleId").value(7L))
                .andExpect(jsonPath("$.paymentStatus").value("APPROVED"));
    }

    @Test
    @WithMockUser(authorities = "CREAR_VENTAS")
    void registrarVenta_sinStock_devuelve400() throws Exception {
        // Regla de negocio (stock insuficiente) → SaleException → 400 SALE_ERROR.
        when(saleService.processSale(any()))
                .thenThrow(new SaleException("Stock insuficiente para el producto Café"));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SALE_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "VER_VENTAS")
    void listarVentas_devuelveHistorial() throws Exception {
        com.erikjarquin.ventas.model.dto.Sale.SaleHistoryResponse item =
                new com.erikjarquin.ventas.model.dto.Sale.SaleHistoryResponse();
        item.setId(1L);
        item.setSaleDate(LocalDateTime.now());
        item.setTotal(new BigDecimal("50.00"));
        item.setPaymentStatus(PaymentStatus.APPROVED);

        when(saleService.getSales()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @WithMockUser(authorities = "VER_VENTAS")
    void obtenerVentaPorId_devuelveDetalle() throws Exception {
        com.erikjarquin.ventas.model.dto.Sale.SaleDetailHistoryResponse detalle =
                new com.erikjarquin.ventas.model.dto.Sale.SaleDetailHistoryResponse();
        detalle.setSaleId(5L);
        detalle.setTotal(new BigDecimal("80.00"));
        detalle.setItems(List.of());

        when(saleService.getSaleById(5L)).thenReturn(detalle);

        mockMvc.perform(get("/api/sales/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleId").value(5L));
    }

    @Test
    @WithMockUser(authorities = "OTRO")
    void registrarVenta_sinPermiso_devuelve403() throws Exception {
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"items\":[]}"))
                .andExpect(status().isForbidden());

        verify(saleService, never()).processSale(any());
        verify(saleService, never()).getSaleById(anyLong());
    }
}
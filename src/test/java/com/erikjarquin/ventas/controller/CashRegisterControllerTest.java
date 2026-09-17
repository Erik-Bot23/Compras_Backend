package com.erikjarquin.ventas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.erikjarquin.ventas.config.JwtUtil;
import com.erikjarquin.ventas.config.SecurityConfig;
import com.erikjarquin.ventas.config.security.SecurityAuthorityMapper;
import com.erikjarquin.ventas.exceptions.CashException;
import com.erikjarquin.ventas.model.dto.Cash.CashResponse;
import com.erikjarquin.ventas.model.dto.Cash.CashSummaryResponse;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.CashRegisterService;

/**
 * Tests del controlador de caja registradora.
 *
 * <p>Cubre apertura, cierre, resumen (hoja de corte) y consulta de caja activa.
 * También valida el permiso específico de cada endpoint (ABRIR_CAJA / CERRAR_CAJA
 * / CORTE_CAJA / VER_CAJA).
 */
@WebMvcTest(CashRegisterController.class)
@Import(SecurityConfig.class)
class CashRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashRegisterService cashService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    private CashResponse cajaActiva() {
        CashResponse response = new CashResponse();
        response.setId(1L);
        response.setOpeningAmount(new BigDecimal("500.00"));
        response.setActive(true);
        response.setTotalSales(new BigDecimal("1230.50"));
        return response;
    }

    @Test
    @WithMockUser(authorities = "ABRIR_CAJA")
    void abrirCaja_devuelveCaja() throws Exception {
        when(cashService.open(any())).thenReturn(cajaActiva());

        mockMvc.perform(post("/api/cash/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingAmount\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(authorities = "CERRAR_CAJA")
    void cerrarCaja_devuelveCajaCerrada() throws Exception {
        CashResponse cerrada = cajaActiva();
        cerrada.setActive(false);
        when(cashService.close(any())).thenReturn(cerrada);

        mockMvc.perform(post("/api/cash/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingAmount\":1700}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(authorities = "VER_CAJA")
    void consultarCajaActiva_devuelveCaja() throws Exception {
        when(cashService.getActiveCash()).thenReturn(cajaActiva());

        mockMvc.perform(get("/api/cash/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(authorities = "CORTE_CAJA")
    void obtenerResumen_devuelveHojaDeCorte() throws Exception {
        CashSummaryResponse summary = new CashSummaryResponse();
        summary.setCashId(1L);
        summary.setOpeningAmount(new BigDecimal("500.00"));
        summary.setTotalSales(new BigDecimal("1230.50"));
        summary.setExpectedAmount(new BigDecimal("1730.50"));
        summary.setTotalTickets(7);

        when(cashService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/cash/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashId").value(1L))
                .andExpect(jsonPath("$.totalTickets").value(7));
    }

    @Test
    @WithMockUser(authorities = "ABRIR_CAJA")
    void cerrarCaja_sinPermisoDeCierre_devuelve403() throws Exception {
        // Tiene ABRIR_CAJA pero no CERRAR_CAJA → el método queda bloqueado.
        mockMvc.perform(post("/api/cash/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingAmount\":1700}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "VER_CAJA")
    void consultarCajaSinCajaAbierta_devuelve404() throws Exception {
        when(cashService.getActiveCash())
                .thenThrow(new CashException("No hay caja abierta", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/cash/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CASH_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ABRIR_CAJA")
    void abrirCajaYaAbierta_devuelve409() throws Exception {
        when(cashService.open(any()))
                .thenThrow(new CashException("Ya existe una caja abierta", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/cash/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingAmount\":500}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASH_ERROR"));

        verify(cashService).open(any());
    }
}
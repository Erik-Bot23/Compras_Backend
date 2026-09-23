package com.erikjarquin.test.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.erikjarquin.ventas.config.JwtUtil;
import com.erikjarquin.ventas.config.SecurityConfig;
import com.erikjarquin.ventas.config.security.SecurityAuthorityMapper;
import com.erikjarquin.ventas.controller.ReportController;
import com.erikjarquin.ventas.model.dto.Reports.PeriodSalesDTO;
import com.erikjarquin.ventas.model.dto.Reports.ReportsSummaryDTO;
import com.erikjarquin.ventas.model.enums.ReportGroup;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.ReportsService;

/**
 * Tests del controlador de reportes (dashboard).
 *
 * <p>Verifica los principales endpoints y el enrutado de parámetros opcionales
 * (from/to/groupBy), además del permiso único VER_REPORTES compartido por todos.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportsService reportsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    @Test
    @WithMockUser(authorities = "VER_REPORTES")
    void tendencia_deVentas_devuelvePuntos() throws Exception {
        when(reportsService.getSalesTrend(isNull(), isNull(), any(ReportGroup.class)))
                .thenReturn(List.of(
                        new PeriodSalesDTO("2026-09", new BigDecimal("1500.00"), 12L),
                        new PeriodSalesDTO("2026-10", new BigDecimal("1800.00"), 15L)));

        mockMvc.perform(get("/api/reports/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].period").value("2026-09"));

        verify(reportsService).getSalesTrend(isNull(), isNull(), any(ReportGroup.class));
    }

    @Test
    @WithMockUser(authorities = "VER_REPORTES")
    void topProductos_respetaElLimite() throws Exception {
        // IMPORTANTE: si usas matchers (isNull), TODOS los argumentos deben ser
        // matchers. El límite va con eq(10), no con el valor crudo "10".
        when(reportsService.getTopProducts(isNull(), isNull(), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/top-products").param("limit", "10"))
                .andExpect(status().isOk());

        verify(reportsService).getTopProducts(isNull(), isNull(), eq(10));
    }

    @Test
    @WithMockUser(authorities = "VER_REPORTES")
    void stockBajo_conUmbral_devuelveProductos() throws Exception {
        when(reportsService.getLowStock(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/low-stock").param("threshold", "5"))
                .andExpect(status().isOk());

        verify(reportsService).getLowStock(5);
    }

    @Test
    @WithMockUser(authorities = "VER_REPORTES")
    void resumen_devuelveTotales() throws Exception {
        ReportsSummaryDTO summary = new ReportsSummaryDTO();
        summary.setTotalSales(100L);
        summary.setTotalAmount(new BigDecimal("50000.00"));

        when(reportsService.getSummary(isNull(), isNull())).thenReturn(summary);

        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(100L));
    }

    @Test
    @WithMockUser(authorities = "OTRO_PERMISO")
    void reportes_sinPermiso_devuelve403() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isForbidden());

        verify(reportsService, never()).getSummary(any(), any());
        verify(reportsService, never()).getLowStock(anyInt());
    }
}
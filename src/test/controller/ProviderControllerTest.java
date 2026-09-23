package com.erikjarquin.test.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import com.erikjarquin.ventas.controller.ProviderController;
import com.erikjarquin.ventas.exceptions.ProviderException;
import com.erikjarquin.ventas.model.dto.Purchases.ProviderDto;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.ProviderService;

/**
 * Tests del controlador de proveedores.
 *
 * <p>Verifica el CRUD y las reglas de negocio clave: RFC duplicado → 409 y
 * proveedor con compras asociadas → 409 (no se puede borrar).
 */
@WebMvcTest(ProviderController.class)
@Import(SecurityConfig.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderService providerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    private ProviderDto proveedor() {
        ProviderDto dto = new ProviderDto();
        dto.setId(1L);
        dto.setName("Proveedor Alfa");
        dto.setRfc("AAA010101AAA");
        dto.setEmail("contacto@alfa.com");
        return dto;
    }

    @Test
    @WithMockUser(authorities = "VER_PROVEEDORES")
    void listarProveedores_devuelveLista() throws Exception {
        when(providerService.getAll()).thenReturn(List.of(proveedor()));

        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rfc").value("AAA010101AAA"));
    }

    @Test
    @WithMockUser(authorities = "VER_PROVEEDORES")
    void obtenerProveedorPorId_devuelveProveedor() throws Exception {
        when(providerService.getById(1L)).thenReturn(proveedor());

        mockMvc.perform(get("/api/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Proveedor Alfa"));
    }

    @Test
    @WithMockUser(authorities = "CREAR_PROVEEDORES")
    void crearProveedor_devuelve201() throws Exception {
        when(providerService.save(any())).thenReturn(proveedor());

        mockMvc.perform(post("/api/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Alfa\",\"rfc\":\"AAA010101AAA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(authorities = "CREAR_PROVEEDORES")
    void crearProveedor_rfcDuplicado_devuelve409() throws Exception {
        when(providerService.save(any()))
                .thenThrow(new ProviderException("Ya existe un proveedor con ese RFC", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Alfa\",\"rfc\":\"AAA010101AAA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "EDITAR_PROVEEDORES")
    void editarProveedor_devuelveProveedor() throws Exception {
        when(providerService.update(anyLong(), any())).thenReturn(proveedor());

        mockMvc.perform(put("/api/providers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Alfa\",\"rfc\":\"AAA010101AAA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Proveedor Alfa"));
    }

    @Test
    @WithMockUser(authorities = "ELIMINAR_PROVEEDORES")
    void eliminarProveedor_sinCompras_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/providers/1"))
                .andExpect(status().isNoContent());

        verify(providerService).delete(1L);
    }

    @Test
    @WithMockUser(authorities = "ELIMINAR_PROVEEDORES")
    void eliminarProveedor_conCompras_devuelve409() throws Exception {
        doThrow(new ProviderException("No puedes eliminar un proveedor con compras registradas",
                HttpStatus.CONFLICT))
                .when(providerService).delete(1L);

        mockMvc.perform(delete("/api/providers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"));
    }
}
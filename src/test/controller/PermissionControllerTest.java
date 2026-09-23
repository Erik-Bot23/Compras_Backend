package com.erikjarquin.test.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.erikjarquin.ventas.controller.PermissionController;
import com.erikjarquin.ventas.model.dto.Permissions.PermissionResponse;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.PermissionService;

/**
 * Tests del controlador de permisos (catálogo de lectura para el frontend).
 * Reutiliza el permiso VER_ROLES (compartido con el módulo de roles).
 */
@WebMvcTest(PermissionController.class)
@Import(SecurityConfig.class)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    @Test
    @WithMockUser(authorities = "VER_ROLES")
    void listarPermisos_devuelveCatalogo() throws Exception {
        when(permissionService.getAllPermissions()).thenReturn(List.of(
                new PermissionResponse(1L, "VER_VENTAS"),
                new PermissionResponse(2L, "CREAR_VENTAS")));

        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("CREAR_VENTAS"));
    }

    @Test
    @WithMockUser(authorities = "OTRO")
    void listarPermisos_sinPermiso_devuelve403() throws Exception {
        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isForbidden());

        verify(permissionService, never()).getAllPermissions();
    }
}
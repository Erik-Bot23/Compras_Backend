package com.erikjarquin.test.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.erikjarquin.ventas.controller.UserController;
import com.erikjarquin.ventas.exceptions.UserException;
import com.erikjarquin.ventas.model.dto.User.UserDto;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.UserService;

/**
 * Tests del controlador de usuarios.
 *
 * <p>Verifica el CRUD, los endpoints PATCH/DELETE de activación-desactivación y
 * que cada acción respete su permiso específico (p. ej. DESACTIVAR_USUARIOS).
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SecurityAuthorityMapper authorityMapper;

    private UserDto usuario() {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setName("Erik");
        dto.setEmail("erik@correo.com");
        dto.setRoleId(1L);
        dto.setRoleName("ADMIN");
        dto.setActive(true);
        return dto;
    }

    @Test
    @WithMockUser(authorities = "VER_USUARIOS")
    void listarUsuarios_devuelveLista() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(usuario()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("erik@correo.com"));
    }

    @Test
    @WithMockUser(authorities = "CREAR_USUARIOS")
    void crearUsuario_devuelveUsuario() throws Exception {
        when(userService.createUser(any())).thenReturn(usuario());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Erik\",\"email\":\"erik@correo.com\",\"password\":\"1234\",\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Erik"));
    }

    @Test
    @WithMockUser(authorities = "CREAR_USUARIOS")
    void crearUsuario_emailDuplicado_devuelve409() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new UserException("Ya existe un usuario con ese email", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Erik\",\"email\":\"erik@correo.com\",\"password\":\"1234\",\"roleId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "EDITAR_USUARIOS")
    void editarUsuario_devuelveUsuario() throws Exception {
        when(userService.updateUser(anyLong(), any())).thenReturn(usuario());

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Erik\",\"email\":\"erik@correo.com\",\"roleId\":1,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleName").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = "DESACTIVAR_USUARIOS")
    void desactivarUsuario_devuelve200() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk());

        verify(userService).deactivateUser(1L);
    }

    @Test
    @WithMockUser(authorities = "ACTIVAR_USUARIOS")
    void activarUsuario_devuelve200() throws Exception {
        mockMvc.perform(patch("/api/users/1/active"))
                .andExpect(status().isOk());

        verify(userService).activateUser(1L);
    }

    @Test
    @WithMockUser(authorities = "VER_USUARIOS")
    void desactivarUsuario_sinPermisoEspecifico_devuelve403() throws Exception {
        // Tiene VER_USUARIOS pero no DESACTIVAR_USUARIOS → método bloqueado.
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());

        verify(userService, never()).deactivateUser(anyLong());
    }

    @Test
    @WithMockUser(authorities = "DESACTIVAR_USUARIOS")
    void desactivarUsuario_inexistente_devuelve404() throws Exception {
        doThrow(new UserException("Usuario no encontrado"))
                .when(userService).deactivateUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_ERROR"));
    }
}
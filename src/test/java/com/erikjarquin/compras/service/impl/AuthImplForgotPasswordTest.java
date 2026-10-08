package com.erikjarquin.compras.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.erikjarquin.compras.config.JwtUtil;
import com.erikjarquin.compras.model.entity.UserEntity;
import com.erikjarquin.compras.repository.UserRepository;
import com.erikjarquin.compras.service.EmailSender;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthImplForgotPasswordTest {
    @Mock
    private UserRepository repo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private AuthImpl authImpl;

    @Captor
    private ArgumentCaptor<String> recipientCaptor;

    @Captor
    private ArgumentCaptor<String> linkCaptor;

    @BeforeEach
    void setUp(){
        /**
         * frontendUrl es @Value (inyección de campo): un test plano queda
         * null, así que se fija a mano
         */
        ReflectionTestUtils.setField(authImpl, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void forgotPassword_enviaElEnlaceConElTokenYLaVigenciaDeUnaHora(){
        UserEntity user = new UserEntity();
        user.setEmail("prueba@ejemplo.com");

        org.mockito.Mockito.when(repo.findByEmail("prueba@ejemplo.com")).thenReturn(Optional.of(user));

        authImpl.forgotPassword("prueba@ejemplo.com");

        //1) Se guarda el usario con token y vencimiento
        org.mockito.Mockito.verify(repo).save(user);
        assertEquals(36, user.getResetToken().length(), "UUID tiene 36 caracteres");
        assertEquals(true, user.getResetTokenExpiration().isAfter(LocalDateTime.now()));

        //2) El correo sale por la interfaz (no por JavaMailSender)
        verify(emailSender).sendPasswordRecoveryEmail(recipientCaptor.capture(), linkCaptor.capture());
        assertEquals("prueba@ejemplo.com", recipientCaptor.getValue());
        assertEquals(true, linkCaptor.getValue().startsWith("http://localhost:4200/reset-password?token="));
        assertEquals(true, linkCaptor.getValue().endsWith(user.getResetToken()));
    }

    @Test
    void forgotPassword_correoNoRegistrado_noIntentaEnviarNiGuardar(){
        org.mockito.Mockito.when(repo.findByEmail("nadie@ejemplo.com")).thenReturn(Optional.empty());
        
        org.junit.jupiter.api.Assertions.assertThrows(com.erikjarquin.compras.exceptions.UserException.class, () -> authImpl.forgotPassword("nadie@ejemplo.com"));

        verify(emailSender, never()).sendPasswordRecoveryEmail(anyString(), anyString());
        verify(repo, never()).save(any());

    }
}

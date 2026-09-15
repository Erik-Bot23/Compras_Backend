package com.erikjarquin.ventas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erikjarquin.ventas.model.dto.Login.LoginRequest;
import com.erikjarquin.ventas.model.dto.Login.LoginResponse;
import com.erikjarquin.ventas.model.dto.ResetPassword.ChangePasswordRequest;
import com.erikjarquin.ventas.model.dto.ResetPassword.ForgotPasswordRequest;
import com.erikjarquin.ventas.model.dto.ResetPassword.ResetPasswordRequest;
import com.erikjarquin.ventas.model.entity.UserEntity;
import com.erikjarquin.ventas.service.AuthService;

/**
 * Autenticación y gestión de contraseñas (login, recuperación y cambio).
 *
 * <p>Es el único grupo de endpoints marcados como PÚBLICOS en SecurityConfig
 * ({@code /api/auth/**} → permitAll), pues el login/recuperación ocurre antes de
 * tener un token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service){
        this.service = service;
    }

    //Login para el usuario
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        return service.login(request);
    }

    //Si la contraseña ha sido olvidada
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req){
        service.forgotPassword(req.getEmail());

        return ResponseEntity.ok().build();
    }

    //Resetear la contraseña
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req){
        service.resetPassword(req.getToken(), req.getNewPassword());

        return ResponseEntity.ok().build();
    }

    //Cambiar contraseña
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req, Authentication auth){
        UserEntity user = (UserEntity) auth.getPrincipal();

        service.changePassword(user.getEmail(), req);

        return ResponseEntity.ok().build();
    }
}

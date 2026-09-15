package com.erikjarquin.ventas.service.impl;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.config.JwtUtil;
import com.erikjarquin.ventas.exceptions.UserException;
import com.erikjarquin.ventas.model.dto.Login.LoginRequest;
import com.erikjarquin.ventas.model.dto.Login.LoginResponse;
import com.erikjarquin.ventas.model.dto.ResetPassword.ChangePasswordRequest;
import com.erikjarquin.ventas.model.entity.UserEntity;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.AuthService;
import com.erikjarquin.ventas.service.EmailService;

/**
 * Implementación del módulo de AUTH:
 *  - login: valida credenciales (usuarios activos), devuelve el JWT junto con
 *    el rol y los permisos. Falla "en silencio" (LoginResponse con success=false)
 *    para no revelar si el correo existe o no (evita enumeración de usuarios).
 *  - forgot/reset-password: token UUID con 1 hora de validez; el enlace apunta
 *    a ${app.frontend-url} (local: localhost:4200, prod: Dominio Netlify).
 *  - change-password: exige la contraseña actual antes de cambiarla.
 */
@Service
public class AuthImpl implements AuthService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.frontend-url}") //
    private String frontendUrl;

    public AuthImpl(
        UserRepository repo, 
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        EmailService emailService){
        this.repo=repo;
        this.passwordEncoder=passwordEncoder;
        this.jwtUtil=jwtUtil;
        this.emailService=emailService;
    }

    //Realizar el login
    @Override
    public LoginResponse login(LoginRequest request){

        return repo.findByEmailWithRoleAndPermissions(request.getEmail())
                .filter(UserEntity::isActive) //
                .filter(user -> passwordEncoder.matches( //
                    request.getPassword(),
                    user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user);

                    List<String> permissions = user.getRole().getPermissions().stream().map(permission -> permission.getName().name()).toList();

                    return new LoginResponse(
                        true,
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().getName(),
                        permissions,
                        token
                    ); 
                })
                .orElse(
                    new LoginResponse(
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                );
    }

    //Olvidar contraseña
    @Override
    public void forgotPassword(String email){
        UserEntity user = repo.findByEmail(email).orElseThrow(() ->
            new UserException("No existe una cuenta registrada con ese correo electrónico."));

        String token = UUID.randomUUID().toString(); //

        user.setResetToken(token);
        user.setResetTokenExpiration(LocalDateTime.now().plusHours(1));

        repo.save(user);

        String link = frontendUrl + "/reset-password?token=" + token;

        emailService.sendPasswordRecoveryEmail(user.getEmail(), link);

    }

    //Resetear contraseña
    @Override
    public void resetPassword(String token, String newPassword){
        UserEntity user = repo.findByResetToken(token).orElseThrow(() ->
            new UserException("Token inválido", HttpStatus.BAD_REQUEST));

        //isBefore
        if(user.getResetTokenExpiration().isBefore(LocalDateTime.now())){
            throw new UserException("Token expirado", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiration(null);

        repo.save(user);
    }

    //Cambiar contraseña
    @Override
    public void changePassword(String email, ChangePasswordRequest request){
        UserEntity user = repo.findByEmail(email).orElseThrow();

        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())){
            throw new UserException("Contraseña actual incorrecta", HttpStatus.BAD_REQUEST);
        }

        //encode
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        repo.save(user);
    }
}

package com.erikjarquin.compras.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erikjarquin.compras.exceptions.UserException;
import com.erikjarquin.compras.mapper.UserMapper;
import com.erikjarquin.compras.model.dto.User.CreateUserRequest;
import com.erikjarquin.compras.model.dto.User.UpdateUserRequest;
import com.erikjarquin.compras.model.dto.User.UserDto;
import com.erikjarquin.compras.model.entity.RoleEntity;
import com.erikjarquin.compras.model.entity.UserEntity;
import com.erikjarquin.compras.repository.RoleRepository;
import com.erikjarquin.compras.repository.UserRepository;
import com.erikjarquin.compras.service.UserService;
import com.erikjarquin.compras.util.InputValidator;

/**
 * Implementación de usuarios. Reglas de negocio:
 *  - Las contraseñas SIEMPRE se guardan con BCrypt (nunca en texto plano).
 *  - Un usuario no se borra físicamente: se desactiva (active=false) para
 *    conservar la integridad referencial con ventas/pagos históricos.
 *  - El email debe ser único en el sistema.
 */
@Service
public class UserImpl implements UserService {
    private final UserRepository repository;
     private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserImpl(UserRepository repository, 
        PasswordEncoder passwordEncoder,
        RoleRepository roleRepository){
        this.repository=repository;
        this.passwordEncoder=passwordEncoder;
        this.roleRepository=roleRepository;
    }

    //Ver usuarios
   @Override
    public List<UserDto> getAllUsers() {
        return repository.findAll().stream().map(UserMapper::toDto).toList();
    }

    //Crear usuario
    @Override
    public UserDto createUser(CreateUserRequest request){
        //El correo no debe estar ya registrado (sería violación de unique).
        if(repository.findByEmail(request.getEmail()).isPresent()){
            throw new UserException("El correo ya está registrado", org.springframework.http.HttpStatus.CONFLICT);
        }

        UserEntity user = new UserEntity();

        String userName = InputValidator.texto(request.getName(), "nombre del usuario", 50);
        String emailUser = InputValidator.email(request.getEmail());
        user.setName(userName);
        user.setEmail(emailUser);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        RoleEntity role = roleRepository.findById(request.getRoleId()).orElseThrow(() -> 
            new IllegalArgumentException("Rol no encontrado"));
        user.setRole(role);
        user.setActive(true);
        //"Dado de alta" = el momento en que se creó la cuenta (V5). Es la fecha
        //que responde "¿desde cuándo trabaja con nosotros?".
        user.setActivatedAt(LocalDateTime.now());
        user.setDeactivatedAt(null);

        UserEntity saved = repository.save(user);

        return UserMapper.toDto(saved);
    }

    //Actualizar usuario
    @Override
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = repository.findById(id).orElseThrow(() ->
            new UserException("Usuario no encontrado"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        RoleEntity role = roleRepository.findById(request.getRoleId()).orElseThrow(() -> 
            new IllegalArgumentException("Rol no encontrado"));
        user.setRole(role);
        UserEntity updated = repository.save(user);

        return UserMapper.toDto(updated);
    }

    //Desactivar usuario
    @Override
    public void deactivateUser(Long id) {
        UserEntity user = repository.findById(id).orElseThrow(() ->
            new UserException("Usuario no encontrado"));

        //Si ya estaba dado de baja no se pisa la fecha: se conserva la baja REAL
        //(la primera), que es la que responde "¿desde cuándo se fue?". Si se
        //sobrescribiera con cada llamada, al repetir la operación se perdería el
        //dato original.
        if(user.isActive()){
            user.setDeactivatedAt(LocalDateTime.now());
        }

        //Al reactivarse después, la fecha de baja deja de ser la vigente: se
        //limpia para que el filtro "dados de baja por fecha" no lo encuentre.
        user.setActivatedAt(null);
        user.setActive(false);
        repository.save(user);
    }

    //Activar usuario (y re-activar uno que estaba dado de baja)
    @Override
    public void activateUser(Long id){
        UserEntity user = repository.findById(id).orElseThrow(() ->
            new UserException("Usuario no encontrado"));

        //Siempre se refresca: re-activar es un evento nuevo ("volvió el día X"),
        // no una restauración del alta original.
        user.setActivatedAt(LocalDateTime.now());
        user.setDeactivatedAt(null);
        user.setActive(true);
        repository.save(user);
    }
}

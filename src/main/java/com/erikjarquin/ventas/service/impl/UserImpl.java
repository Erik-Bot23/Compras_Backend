package com.erikjarquin.ventas.service.impl;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erikjarquin.ventas.exceptions.UserException;
import com.erikjarquin.ventas.mapper.UserMapper;
import com.erikjarquin.ventas.model.dto.User.CreateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UpdateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UserDto;
import com.erikjarquin.ventas.model.entity.RoleEntity;
import com.erikjarquin.ventas.model.entity.UserEntity;
import com.erikjarquin.ventas.repository.RoleRepository;
import com.erikjarquin.ventas.repository.UserRepository;
import com.erikjarquin.ventas.service.UserService;

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

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        RoleEntity role = roleRepository.findById(request.getRoleId()).orElseThrow(() -> 
            new IllegalArgumentException("Rol no encontrado"));
        user.setRole(role);
        user.setActive(true);

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

        user.setActive(false);
        repository.save(user);
    }

    //Activar usuario
    @Override
    public void activateUser(Long id){
        UserEntity user = repository.findById(id).orElseThrow(() ->
            new UserException("Usuario no encontrado"));

        user.setActive(true);
        repository.save(user);
    }
}

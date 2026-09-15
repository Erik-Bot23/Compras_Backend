package com.erikjarquin.ventas.service;

import java.util.List;

import com.erikjarquin.ventas.model.dto.User.CreateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UpdateUserRequest;
import com.erikjarquin.ventas.model.dto.User.UserDto;

/**
 * Contrato de operaciones sobre usuarios del sistema (CRUD + activación).
 * Ver implementación en {@code service/impl/UserImpl}.
 */
public interface UserService {
    List<UserDto> getAllUsers();
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(Long id, UpdateUserRequest request);
    void deactivateUser(Long id);
    void activateUser(Long id);
}

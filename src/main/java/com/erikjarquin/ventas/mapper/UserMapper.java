package com.erikjarquin.ventas.mapper;

import com.erikjarquin.ventas.model.dto.User.UserDto;
import com.erikjarquin.ventas.model.entity.UserEntity;

/**
 * Mapper estático usuario → DTO.
 * NUNCA expone el password (ni siquiera hasheado) fuera de la capa de servicio.
 */
public class UserMapper {
    public static UserDto toDto(UserEntity entity){
        if(entity == null) return null;

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setActive(entity.isActive());

        if (entity.getRole() != null) {
            dto.setRoleId(entity.getRole().getId());
            dto.setRoleName(entity.getRole().getName());
        }
        
        return dto;
    }
}

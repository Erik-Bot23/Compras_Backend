package com.erikjarquin.ventas.config.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.PermissionEntity;
import com.erikjarquin.ventas.model.entity.UserEntity;

/**
 * Convierte el rol y los permisos de un usuario en autoridades de Spring Security.
 *
 * <p>La lista resultante alimenta el {@code Authentication} que crea JwtFilter.
 * Formato:
 *  - {@code ROLE_<NOMBRE_ROL>} (para hasRole("...")).
 *  - el nombre de cada permiso del rol (para hasAuthority("...")).
 *
 * <p>IMPORTANTE: las autoridades se recalculan en cada request desde la BD
 * (findByEmailWithRoleAndPermissions). Si se quita un permiso al rol, el cambio
 * aplica de inmediato, sin esperar la expiración del JWT.
 */
@Component
public class SecurityAuthorityMapper {

    /**
     * @param user usuario autenticado (con su rol y permisos ya cargados)
     * @return lista de GrantedAuthority (rol + permisos)
     */
    public List<GrantedAuthority> mapAuthorities(UserEntity user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Autoridad del rol: ROLE_ADMIN / ROLE_CAJERO / ROLE_ALMACENISTA
        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())
        );

        // Una autoridad por cada permiso del rol (VER_PRODUCTOS, CREAR_VENTAS, ...)
        for (PermissionEntity permission : user.getRole().getPermissions()) {
            authorities.add(
                    new SimpleGrantedAuthority(permission.getName().name())
            );
        }

        return authorities;
    }
}

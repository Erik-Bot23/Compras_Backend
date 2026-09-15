package com.erikjarquin.ventas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erikjarquin.ventas.model.entity.UserEntity;

/**
 * Repositorio de usuarios.
 *
 * <p>findByEmailWithRoleAndPermissions usa JOIN FETCH para cargar rol y
 * permisos en UNA consulta (evita N+1 y errores de LazyInitialization en
 * el AuthenticationProvider, donde se construyen las autoridades).
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    //
    @Query("""
        SELECT DISTINCT u
            FROM UserEntity u
                JOIN FETCH u.role r
                JOIN FETCH r.permissions
                    WHERE u.email = :email
        """)
    
    //Encontrar usuario con email y permisos
    Optional<UserEntity> findByEmailWithRoleAndPermissions(@Param("email") String email);
    
    //Resetear el token
    Optional<UserEntity> findByResetToken(String resetToken);

    //¿Cuántos usuarios tiene un rol? (para bloquear su borrado con 409)
    long countByRole_Id(Long roleId);
}

package com.erikjarquin.ventas.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.erikjarquin.ventas.config.security.SecurityAuthorityMapper;
import com.erikjarquin.ventas.model.entity.UserEntity;
import com.erikjarquin.ventas.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro JWT que intercepta CADA request antes de llegar al controller.
 *
 * FLUJO DE AUTENTICACIÓN en esta API (de arriba a abajo):
 *  1. El cliente envía {@code Authorization: Bearer <token>} en el header.
 *  2. Este filtro (antes de UsernamePasswordAuthenticationFilter):
 *     a. Extrae el token del header.
 *     b. Extrae el email (subject) del token usando JwtUtil.
 *     c. Si el token es válido, busca el usuario completo en BD (con rol +
 *        permisos) y crea un {@code UsernamePasswordAuthenticationToken}
 *        que Spring Security guardará en el SecurityContext.
 *  3. A partir de aquí, cualquier {@code @PreAuthorize} puede verificar
 *     permisos vía {@code hasAuthority("CREAR_VENTAS")}.
 *
 * Si NO hay header o el token está expirado/inválido, el request pasa sin
 * autenticación y los endpoints protegidos retornan 401/403 normalmente.
 *
 * PERMISOS vs ROL: los permisos se cargan SIEMPRE desde la BD (no del JWT),
 * así que si quitas un permiso a un rol, el cambio aplica de inmediato sin
 * esperar la expiración del token (el JWT solo almacena el email del usuario).
 */
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SecurityAuthorityMapper authorityMapper;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository, SecurityAuthorityMapper authorityMapper){
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authorityMapper=authorityMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // 1) Sin header "Authorization" (o sin prefijo Bearer) → seguimos la cadena
        //    sin autenticación. Los endpoints protegidos responderán 401/403.
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2) Extraer el token (después de "Bearer ") y su email (subject).
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            // 3) Solo autenticar si el token es válido y aún no hay sesión en este request.
            if(jwtUtil.isTokenValid(token)){
                if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
                    // 4) Cargar el usuario COMPLETO desde BD (rol + permisos).
                    //    Esto se hace en CADA request: cualquier cambio de
                    //    permisos refleja de inmediato, sin esperar del JWT.
                    UserEntity user = userRepository.findByEmailWithRoleAndPermissions(email).orElse(null);

                    if(user != null){
                        // 5) Construir autenticación con las autoridades (ROLE_* + permisos)
                        //    y depositarla en el SecurityContext para los @PreAuthorize.
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, authorityMapper.mapAuthorities(user));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch(JwtException | IllegalArgumentException e) {
            // Token inválido: expirado, malformado o firmado con otro secreto
            // (SignatureException). No lanzamos 500: limpiamos contexto y dejamos
            // seguir la cadena sin autenticar → los endpoints protegidos dan 401.
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // 6) Continuar con el resto de la cadena (controller, @PreAuthorize, etc.).
        filterChain.doFilter(request, response);
    }
}

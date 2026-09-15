package com.erikjarquin.ventas.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.erikjarquin.ventas.model.entity.PermissionEntity;
import com.erikjarquin.ventas.model.entity.RoleEntity;
import com.erikjarquin.ventas.model.enums.PermissionName;
import com.erikjarquin.ventas.repository.PermissionRepository;
import com.erikjarquin.ventas.repository.RoleRepository;

/**
 * Bootstrap (@Order 4) que asigna permisos a los roles creados:
 *  - ADMIN      → TODOS los permisos.
 *  - CAJERO     → ver/crear ventas, ver productos, abrir/cerrar caja.
 *  - ALMACENISTA→ ver/crear/editar productos + ver/crear compras y ver
 *                 proveedores (gestiona reposición de inventario).
 *
 * <p>Idempotente y SELF-HEALING: en cada arranque se REASIGNA el set de
 * permisos definido aquí. Si en el futuro cambias estas listas, los roles se
 * actualizan solos al reiniciar (sincronizan lo que agregues o quites).
 */
@Component
@Order(4)
@Transactional
public class RolePermissionBootstrap implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionBootstrap(RoleRepository roleRepository, PermissionRepository permissionRepository){
        this.roleRepository=roleRepository;
        this.permissionRepository=permissionRepository;
    }

    @Override
    public void run(String...args){
        RoleEntity admin = roleRepository.findByName("ADMIN").orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
        RoleEntity cajero = roleRepository.findByName("CAJERO").orElseThrow(() -> new RuntimeException("Rol CAJERO no encontrado"));
        RoleEntity almacenista = roleRepository.findByName("ALMACENISTA").orElseThrow(() -> new RuntimeException("Rol ALMACENISTA no encontrado"));

        List<PermissionEntity> allPermissions = permissionRepository.findAll();

        // Se reasigna TODO en cada arranque: si la lista de abajo cambia, los
        // permisos de los roles se sincronizan solos (idempotente).
        admin.setPermissions(new HashSet<>(allPermissions));
        roleRepository.save(admin);

        cajero.setPermissions(filterPermissions(
                allPermissions,
                PermissionName.VER_PRODUCTOS,
                PermissionName.VER_VENTAS,
                PermissionName.CREAR_VENTAS,
                PermissionName.ABRIR_CAJA,
                PermissionName.CERRAR_CAJA
            )
        );
        roleRepository.save(cajero);

        almacenista.setPermissions(filterPermissions(
                allPermissions,
                PermissionName.VER_PRODUCTOS,
                PermissionName.CREAR_PRODUCTOS,
                PermissionName.EDITAR_PRODUCTOS,
                PermissionName.VER_COMPRAS,
                PermissionName.CREAR_COMPRAS,
                PermissionName.VER_PROVEEDORES
            )
        );
        roleRepository.save(almacenista);
    }

    private Set<PermissionEntity> filterPermissions(List<PermissionEntity> permissions,PermissionName...permissionNames){
        Set<PermissionName> requested = Set.of(permissionNames);
        return permissions.stream().filter(permission -> requested.contains(permission.getName())).collect(Collectors.toSet());
    }
}

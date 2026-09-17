package com.erikjarquin.ventas.config;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
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
 * Bootstrap (@Order 4) que asigna los permisos base a los roles iniciales:
 *  - ADMIN      → TODOS los permisos.
 *  - CAJERO     → ver/crear ventas, ver productos, abrir/cerrar caja.
 *  - ALMACENISTA→ ver/crear/editar productos + ver/crear compras y ver
 *                 proveedores (gestiona reposición de inventario).
 *
 * <p><b>Regla (SEED INICIAL, no self-healing):</b> solo asigna permisos si los
 * roles base todavía NO tienen NINGÚN permiso (p. ej. BD recién creada o
 * arranque inicial). Si ya hay al menos un permiso asignado a cualquier rol
 * base, NO se toca nada: respeta la gestión manual (quitar permisos a CAJERO,
 * crear roles personalizados, etc.). Antes este bootstrap REASIGNABA en cada
 * arranque, lo que "resucitaba" roles/permisos borrados a mano.
 *
 * <p>Roles borrados a mano (p. ej. CAJERO) NO se recrean: se omiten y se
 * asignan permisos a los que sí existen.
 *
 * <p>Se respeta {@code app.seed-bootstraps}: en false este bootstrap no hace nada.
 */
@Component
@Order(4)
@Transactional
public class RolePermissionBootstrap implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final boolean seedEnabled;

    public RolePermissionBootstrap(RoleRepository roleRepository,
                                   PermissionRepository permissionRepository,
                                   @Value("${app.seed-bootstraps:true}") boolean seedEnabled){
        this.roleRepository=roleRepository;
        this.permissionRepository=permissionRepository;
        this.seedEnabled=seedEnabled;
    }

    @Override
    public void run(String...args){
        // Flag externo desactivado → no tocar la asignación rol-permisos.
        if(!seedEnabled) return;

        List<PermissionEntity> allPermissions = permissionRepository.findAll();
        // Sin permisos en catálogo no hay nada que asignar (permisos aún no sembrados).
        if(allPermissions.isEmpty()) return;

        // Los roles base pueden no existir (el admin pudo borrarlos a mano):
        // se trabaja solo con los presentes, sin recrearlos.
        RoleEntity admin = roleRepository.findByName("ADMIN").orElse(null);
        RoleEntity cajero = roleRepository.findByName("CAJERO").orElse(null);
        RoleEntity almacenista = roleRepository.findByName("ALMACENISTA").orElse(null);

        // Si CUALQUIER rol base ya tiene algún permiso → el seed inicial ya se
        // hizo (o el admin configura a mano). No reasignar en cada arranque.
        boolean yaSembrado = Stream.of(admin, cajero, almacenista)
                .filter(Objects::nonNull)
                .anyMatch(rol -> !rol.getPermissions().isEmpty());
        if(yaSembrado){
            System.out.println("Permisos de roles ya asignados — se omite el seed (gestión manual).");
            return;
        }

        // Primer arranque (BD vacía): asignar los permisos base a cada rol existente.
        if(admin != null){
            admin.setPermissions(new HashSet<>(allPermissions));
            roleRepository.save(admin);
        }
        if(cajero != null){
            cajero.setPermissions(filterPermissions(
                    allPermissions,
                    PermissionName.VER_PRODUCTOS,
                    PermissionName.VER_VENTAS,
                    PermissionName.CREAR_VENTAS,
                    //VER_CAJA es necesario para que el cajero pueda consultar si
                    //hay caja abierta en el POS (GET /api/cash/active). Sin él,
                    //aunque puede ABRIR/CERRAR caja, no puede leer su estado y el
                    //frontend recibe 403 al cargar la pantalla de cobro.
                    PermissionName.VER_CAJA,
                    PermissionName.ABRIR_CAJA,
                    PermissionName.CERRAR_CAJA
                )
            );
            roleRepository.save(cajero);
        }
        if(almacenista != null){
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
    }

    private Set<PermissionEntity> filterPermissions(List<PermissionEntity> permissions,PermissionName...permissionNames){
        Set<PermissionName> requested = Set.of(permissionNames);
        return permissions.stream().filter(permission -> requested.contains(permission.getName())).collect(Collectors.toSet());
    }
}

package com.erikjarquin.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.PermissionEntity;
import com.erikjarquin.ventas.model.enums.PermissionName;
import com.erikjarquin.ventas.repository.PermissionRepository;

/**
 * Bootstrap (@Order 3) que inserta en BD todos los valores del enum
 * {@link PermissionName} (los permisos del sistema) si aún no existen.
 * Al agregar un valor nuevo al enum se crea solo en el siguiente arranque.
 *
 * <p>Se respeta {@code app.seed-bootstraps}: en false este bootstrap no hace nada.
 */
@Component
@Order(3)
public class PermissionBootstrap implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final boolean seedEnabled;

    public PermissionBootstrap(
        PermissionRepository permissionRepository,
        @Value("${app.seed-bootstraps:true}") boolean seedEnabled){
            this.permissionRepository = permissionRepository;
            this.seedEnabled = seedEnabled;
        }

    @Override
    public void run(String...args){
        // Flag externo desactivado → el admin gestiona permisos a mano.
        if(!seedEnabled) return;

        for(PermissionName permissionName : PermissionName.values()){
            if(permissionRepository.findByName(permissionName).isEmpty()) {
                PermissionEntity permission = new PermissionEntity(null, permissionName);
            
                permissionRepository.save(permission);

                System.out.println("Permiso creado: " + permissionName);
            }
        }
    }
}

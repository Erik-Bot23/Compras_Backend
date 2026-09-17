package com.erikjarquin.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.RoleEntity;
import com.erikjarquin.ventas.repository.RoleRepository;

/**
 * Bootstrap (@Order 1) que crea los roles base (ADMIN, CAJERO, ALMACENISTA).
 *
 * <p>Se ejecuta antes que AdminBootstrap (que necesita el rol ADMIN).
 *
 * <p><b>Regla:</b> solo se crean roles si la tabla está VACÍA (primer arranque).
 * Si ya existen roles (por mínima intervención manual o un arranque previo),
 * NO se tocan: los roles que borres a mano se quedan borrados.
 *
 * <p>Se respeta {@code app.seed-bootstraps}: en false este bootstrap no hace nada.
 */
@Component
@Order(1)
public class RoleBootstrap implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final boolean seedEnabled;

    public RoleBootstrap(RoleRepository roleRepository, @Value("${app.seed-bootstraps:true}") boolean seedEnabled){
        this.roleRepository = roleRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String...args){
        // Flag externo desactivado → el desarrollador/admin gestiona roles a mano.
        if(!seedEnabled) return;

        // Solo sembrar si NO hay ningún rol todavía. Esto evita "resucitar"
        // roles borrados a mano (sin este check, borras CAJERO y reaparece).
        if(roleRepository.count() > 0) return;

        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("CAJERO");
        createRoleIfNotExists("ALMACENISTA");
    }

    private void createRoleIfNotExists(String roleName){
        if(roleRepository.findByName(roleName).isEmpty()){
            RoleEntity role = new RoleEntity();
            role.setName(roleName);
            roleRepository.save(role);

            System.out.println("Rol creado " + roleName);
        }
    }
}
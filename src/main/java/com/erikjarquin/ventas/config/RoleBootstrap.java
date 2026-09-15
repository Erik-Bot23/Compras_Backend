package com.erikjarquin.ventas.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.erikjarquin.ventas.model.entity.RoleEntity;
import com.erikjarquin.ventas.repository.RoleRepository;

/**
 * Bootstrap (@Order 1) que crea los roles base si no existen: ADMIN, CAJERO
 * y ALMACENISTA. Se ejecuta antes que AdminBootstrap (que necesita rol ADMIN).
 */
@Component
@Order(1)
public class RoleBootstrap implements CommandLineRunner {
    private final RoleRepository roleRepository;

    public RoleBootstrap(RoleRepository roleRepository){
        this.roleRepository=roleRepository;
    }

    @Override
    public void run(String...args){
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

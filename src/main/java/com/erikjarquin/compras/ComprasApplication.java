package com.erikjarquin.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de Compras-Backend (Spring Boot).
 *
 * @code @EnableScheduling} activa las tareas programadas, como
 * {@code PaymentMonitorJob}, que cada 5 minutos resuelve pagos con tarjeta que
 * quedaron PENDING. El escaneo de componentes (component-scan) cubre
 * automáticamente los paquetes: config, controller, service, repository, model,
 * mapper, exceptions y jobs.
 */
@SpringBootApplication
@EnableScheduling
public class ComprasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComprasApplication.class, args);
	}

}

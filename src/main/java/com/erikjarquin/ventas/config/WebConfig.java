package com.erikjarquin.ventas.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración web de Spring MVC para servir archivos estáticos del backend.
 *
 * <p>Expone la carpeta {@code app.upload-dir} (default: ./uploads) bajo la ruta
 * pública {@code /api/uploads/**}. Así las imágenes de productos guardadas con
 * FileStorageService se sirven como: {@code http://host/api/uploads/<archivo>}.
 *
 * <p>Nota de seguridad: esta ruta es PÚBLICA en SecurityConfig porque las
 * imágenes las muestra el frontend en el navegador sin token. Por eso
 * FileStorageService valida tipo/contenido de lo que se sube.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Carpeta absoluta donde viven los archivos subidos. */
    private final String uploadDir;

    public WebConfig(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
package com.erikjarquin.ventas.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio de almacenamiento de imágenes en disco.
 *
 * <p>Funciona a nivel local y Docker. La carpeta de destino se configura con
 * {@code app.upload-dir} en application.yaml (default: ./uploads).
 *
 * <p>SEGURIDAD: los archivos subidos pueden ser peligrosos si se sirven luego
 * públicamente (como hace {@code /api/uploads/**}). Por eso aquí se valida:
 *   1. Tamaño máximo (5 MB).
 *   2. Extensión permitida (solo imágenes).
 *   3. Contenido REAL mediante "magic bytes" (los primeros bytes del archivo),
 *      para que un `.exe` renombrado a `.jpg` sea rechazado.
 *
 * <p>Los archivos se guardan con un nombre UUID + extensión (nunca con el nombre
 * original del usuario), evitando colisiones y rutas peligrosas (path traversal).
 */
@Service
public class FileStorageService {

    /** Extensiones de imagen permitidas al subir. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /** Tamaño máximo por imagen (5 MB). */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    /** Carpeta física absoluta donde se guardan los archivos. */
    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads: " + this.uploadDir, e);
        }
    }

    /**
     * Guarda la imagen en disco y devuelve el nombre con el que quedó almacenada
     * (UUID + extensión). Devuelve {@code null} si no se envió imagen.
     *
     * @throws IllegalArgumentException si el archivo no es una imagen válida.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1) Validar tamaño
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo de 5 MB");
        }

        // 2) Validar extensión (sanitizando el nombre original)
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        int dot = original.lastIndexOf('.');
        String extension = (dot >= 0) ? original.substring(dot).toLowerCase() : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido: " + (extension.isEmpty() ? "(sin extensión)" : extension));
        }

        // 3) Validar contenido real con magic bytes (evita archivos renombrados)
        if (!isValidImageContent(file)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }

        // 4) Guardar con nombre único e impredecible
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = this.uploadDir.resolve(storedName).normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo: " + storedName, e);
        }
        return storedName;
    }

    /**
     * Elimina el archivo almacenado (si existe). No lanza si no está.
     *
     * @param storedName nombre devuelto previamente por {@link #store(MultipartFile)}
     */
    public void delete(String storedName) {
        if (storedName == null || storedName.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(this.uploadDir.resolve(storedName).normalize());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo: " + storedName, e);
        }
    }

    /**
     * Verifica que los primeros bytes del archivo correspondan a un formato de
     * imagen real (JPEG, PNG, GIF, WebP o BMP). Es la validación fuerte: aunque
     * el atacante mienta sobre el nombre/extensión, la firma del archivo se revela.
     */
    private boolean isValidImageContent(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            return isJpeg(header) || isPng(header) || isGif(header) || isWebp(header) || isBmp(header);
        } catch (IOException e) {
            return false;
        }
    }

    /** JPEG: FFD8 FF */
    private boolean isJpeg(byte[] h) {
        return h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    /** PNG: 89 50 4E 47 0D 0A 1A 0A */
    private boolean isPng(byte[] h) {
        return h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
    }

    /** GIF: "GIF8" */
    private boolean isGif(byte[] h) {
        return h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
    }

    /** WebP: "RIFF" .... "WEBP" */
    private boolean isWebp(byte[] h) {
        return h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }

    /** BMP: "BM" */
    private boolean isBmp(byte[] h) {
        return h.length >= 2 && h[0] == 'B' && h[1] == 'M';
    }
}
package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda las imágenes que se suben desde los formularios en la carpeta uploads
 * (en Docker es el volumen uploads_data), nunca dentro del código.
 */
@Component
public class AlmacenImagenes {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final String carpetaUploads;

    public AlmacenImagenes(@Value("${app.uploads.root:uploads}") String carpetaUploads) {
        this.carpetaUploads = carpetaUploads;
    }

    /** Devuelve la URL pública (/uploads/subcarpeta/archivo). */
    public String guardar(MultipartFile archivo, String subcarpeta) throws IOException {
        String extension = archivo.getContentType() == null ? null : EXTENSIONES.get(archivo.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG, WEBP o GIF");
        }
        Path carpeta = Paths.get(carpetaUploads, subcarpeta).toAbsolutePath();
        Files.createDirectories(carpeta);
        String nombre = UUID.randomUUID() + extension;
        Files.copy(archivo.getInputStream(), carpeta.resolve(nombre));
        return "/uploads/" + subcarpeta + "/" + nombre;
    }

    public void eliminar(String url) {
        if (url == null || !url.startsWith("/uploads/") || url.contains("..")) return;
        try {
            Files.deleteIfExists(Paths.get(carpetaUploads, url.substring("/uploads/".length())).toAbsolutePath());
        } catch (IOException ignored) {
        }
    }
}

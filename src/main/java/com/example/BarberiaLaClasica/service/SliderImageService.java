package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.SliderImage;
import com.example.BarberiaLaClasica.repository.SliderImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class SliderImageService {

    private final SliderImageRepository repo;

    private static final String UPLOAD_DIR = "uploads/slider/";

    public SliderImageService(SliderImageRepository repo) {
        this.repo = repo;
    }

    public List<SliderImage> listarTodas() {
        return repo.findAllByOrderByOrdenAsc();
    }

    public List<SliderImage> listarActivas() {
        return repo.findByActivoTrueOrderByOrdenAsc();
    }

    public SliderImage guardar(MultipartFile archivo, String titulo,
                               String descripcion, Integer orden) throws IOException {
        String extension = "";
        String original = archivo.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf("."));
        }

        String nombreArchivo = UUID.randomUUID() + extension;
        Path destino = Paths.get(UPLOAD_DIR + nombreArchivo);
        Files.createDirectories(destino.getParent());
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        // La URL pública que usará Thymeleaf en th:src
        String url = "/uploads/slider/" + nombreArchivo;

        SliderImage imagen = new SliderImage(url, titulo, descripcion, orden);
        return repo.save(imagen);
    }

    public void toggleActivo(Long id) {
        SliderImage img = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada: " + id));
        img.setActivo(!img.getActivo());
        repo.save(img);
    }

    public void eliminar(Long id) throws IOException {
        SliderImage img = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada: " + id));

        // Elimina el archivo físico
        Path archivo = Paths.get("uploads/slider/" +
                img.getImagenUrl().replace("/uploads/slider/", ""));
        Files.deleteIfExists(archivo);

        repo.delete(img);
    }
}
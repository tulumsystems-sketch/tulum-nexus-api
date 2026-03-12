package com.tulumcore.api.controllers;

import com.tulumcore.api.services.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        // 1. Primer Check: ¿Entró la petición?
        System.out.println(">>> [DEBUG] Petición recibida en /api/media/upload");

        try {
            if (file == null || file.isEmpty()) {
                System.err.println(">>> [DEBUG] El archivo recibido está vacío o es null");
                return ResponseEntity.badRequest().body("Archivo vacío");
            }

            System.out.println(">>> [DEBUG] Archivo: " + file.getOriginalFilename() + " - Tamaño: " + file.getSize());

            String url = cloudinaryService.uploadImage(file);

            System.out.println(">>> [DEBUG] Subida exitosa a Cloudinary. URL: " + url);
            return ResponseEntity.ok("{\"url\": \"" + url + "\"}");

        } catch (Exception e) {
            // 2. Segundo Check: ¿Qué rompió?
            System.err.println(">>> [ERROR CRÍTICO] Falló el proceso de subida:");
            e.printStackTrace(); // Esto imprime las letras rojas que necesito ver
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }
}
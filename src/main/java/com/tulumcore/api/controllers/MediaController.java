package com.tulumcore.api.controllers;

import com.tulumcore.api.services.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo recibido está vacío"));
            }

            String url = cloudinaryService.uploadImage(file);
            return ResponseEntity.ok(Map.of("url", url));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al procesar archivo: " + e.getMessage()));
        }
    }
}
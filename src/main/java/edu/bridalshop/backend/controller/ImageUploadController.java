package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
    public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/dress-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map> uploadDressImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = imageUploadService.uploadImage(file, "dresses");

            Map response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("message", "Image uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
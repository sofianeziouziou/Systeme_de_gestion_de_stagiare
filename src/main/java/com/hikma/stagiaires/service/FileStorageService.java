package com.hikma.stagiaires.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Service de stockage de fichiers.
 * En développement : stockage local dans upload-dir.
 * En production : remplacer par une implémentation AWS S3 / MinIO.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir, folder);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/files/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'upload du fichier", e);
        }
    }

    public void deleteFile(String fileUrl) {
        // TODO: Implémenter suppression locale / S3
        log.info("Suppression fichier : {}", fileUrl);
    }
}
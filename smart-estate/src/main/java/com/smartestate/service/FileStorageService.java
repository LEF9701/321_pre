package com.smartestate.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** Stores issue photos on local disk (no cloud storage in the prototype). */
@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        root = Paths.get(uploadDir);
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String original = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Files.copy(file.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    public Path load(String filename) {
        return root.resolve(filename).normalize();
    }
}

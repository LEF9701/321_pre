package com.smartestate.web;

import com.smartestate.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        try {
            Path file = fileStorageService.load(filename);
            if (!Files.exists(file)) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

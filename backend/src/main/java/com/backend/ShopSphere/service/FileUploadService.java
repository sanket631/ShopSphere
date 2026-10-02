package com.backend.ShopSphere.service;

import com.backend.ShopSphere.exception.IllegalArgsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {
    private static final String UPLOAD_DIR = "uploads/products/";

    public String uploadProductImage(MultipartFile file){
        if(file.isEmpty()){
            log.warn("Requested file to be uploaded is empty");
            throw new IllegalArgsException("File is empty");
        }
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR, fileName);

        try {
            Files.createDirectories(path.getParent());

            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            log.info("Product image uploaded successfully. FileName={}", fileName);
            return "/uploads/products/" + fileName;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getCause().getMessage());
            throw new RuntimeException("File upload failed");
        }
    }
}

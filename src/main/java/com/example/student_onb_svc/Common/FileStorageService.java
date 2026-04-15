package com.example.student_onb_svc.Common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Save a MultipartFile to: uploads/{studentId}/{subfolder}/{filename}
     * Returns the relative path stored in DB.
     */
    public String saveFile(UUID studentId, String subfolder, MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path targetDir = uploadDir.resolve(studentId.toString()).resolve(subfolder);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for DB storage
            String relativePath = studentId + "/" + subfolder + "/" + filename;
            log.info("File saved: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Save a base64-encoded image to: uploads/{studentId}/{subfolder}/{filename}
     * The base64 string can include the data URI prefix (data:image/jpeg;base64,...)
     * Returns the relative path stored in DB.
     */
    public String saveBase64Image(String studentId, String subfolder, String base64Data, String filename) {
        try {
            // Strip data URI prefix if present
            String raw = base64Data;
            if (raw.contains(",")) {
                raw = raw.substring(raw.indexOf(",") + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(raw);

            String storedName = UUID.randomUUID() + "_" + sanitize(filename);
            Path targetDir = uploadDir.resolve(studentId).resolve(subfolder);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedName);
            Files.write(targetPath, imageBytes);

            String relativePath = studentId + "/" + subfolder + "/" + storedName;
            log.info("Base64 image saved: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store base64 image", e);
        }
    }

    /**
     * Get the absolute path for serving a file.
     */
    public Path resolve(String relativePath) {
        return uploadDir.resolve(relativePath);
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
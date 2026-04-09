package com.kaksha.library.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // Ensure upload directories exist on startup
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path imagesPath = uploadPath.resolve("images");
            Path filesPath = uploadPath.resolve("files");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }
            if (!Files.exists(imagesPath)) {
                Files.createDirectories(imagesPath);
                log.info("Created images directory: {}", imagesPath);
            }
            if (!Files.exists(filesPath)) {
                Files.createDirectories(filesPath);
                log.info("Created files directory: {}", filesPath);
            }

            log.info("Upload directory configured at: {}", uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directories: {}", e.getMessage(), e);
        }
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Resolve to absolute path to ensure consistency across app restarts
        Path absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        log.info("Configuring resource handler for /uploads/** -> {}", absoluteUploadPath);

        // Serve uploaded files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Handle legacy image paths (images stored without /uploads/ prefix)
        Path imagesPath = absoluteUploadPath.resolve("images");
        log.info("Configuring resource handler for /images/** -> {}", imagesPath);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagesPath + "/")
                .setCachePeriod(3600); // Cache for 1 hour
    }
}

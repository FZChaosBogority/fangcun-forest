package com.fangcun.forest.admin.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadedImagesConfig implements WebMvcConfigurer {
    private final Path directory;
    public UploadedImagesConfig(@Value("${forest.storage.upload-dir}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = directory.toUri().toString();
        registry.addResourceHandler("/images/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/").setCachePeriod(3600);
    }
}

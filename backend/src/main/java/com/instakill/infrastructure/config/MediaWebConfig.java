package com.instakill.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final Path mediaDir;

    public MediaWebConfig(@Value("${media.base-path:media}") String basePath) {
        this.mediaDir = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaDir.toUri().toString());
    }
}

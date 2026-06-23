package com.rwdenmark.x12.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration so the GitHub Pages portfolio site (and a local Spring Boot dev
 * server) can POST to the parser. Add or remove allowed origins here when the demo
 * moves or gets embedded somewhere new.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "https://rwdenmark.github.io",
                        "https://rdenmark.savannah-luma.ts.net",
                        "http://localhost:8080")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}

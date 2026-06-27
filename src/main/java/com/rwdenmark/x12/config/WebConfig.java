package com.rwdenmark.x12.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS so the GitHub Pages portfolio and local dev server can POST to the API. */
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

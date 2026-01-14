package com.csa.official.config;

import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${csa.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 确保路径格式正确
        String finalPath = uploadPath.endsWith(File.separator) ? uploadPath : uploadPath + File.separator;

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + finalPath);

        System.out.println(" 静态资源映射路径: " + finalPath);
    }
}
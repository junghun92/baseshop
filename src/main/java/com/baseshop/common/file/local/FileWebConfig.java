package com.baseshop.common.file.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local & !local-r2")   // local이면서 local-r2가 아닐 때만
public class FileWebConfig implements WebMvcConfigurer {

    private final String fileDir;

    public FileWebConfig(@Value("${file.dir}") String fileDir) {
        this.fileDir = fileDir;
    }

    // http://localhost:8080/uploads/파일명 -> file.dir 폴더의 파일
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + fileDir);
    }
}

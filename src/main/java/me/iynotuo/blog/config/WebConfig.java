package me.iynotuo.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 将 /images 与 /files 映射到本地上传目录，供 Markdown 直接引用。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${blog.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = Path.of(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + base + "/images/");
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + base + "/files/");
    }
}

package me.iynotuo.blog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@SpringBootApplication
public class BlogPlusApplication {

    private static final Logger log = LoggerFactory.getLogger(BlogPlusApplication.class);

    private static final String CONFIG_FILE = "config.yml";
    private static final String CONFIG_TEMPLATE = "/config.yml.template";

    public static void main(String[] args) {
        ensureConfigFile();
        SpringApplication.run(BlogPlusApplication.class, args);
    }

    /**
     * 首次启动时，从 jar 内置的模板在项目根目录生成 config.yml，
     * 之后所有运行时可配置项（数据库连接、后台凭证、上传目录、头像、备案信息）都从这里读取。
     */
    private static void ensureConfigFile() {
        Path config = Path.of(CONFIG_FILE);
        if (Files.exists(config)) {
            return;
        }
        try (InputStream in = BlogPlusApplication.class.getResourceAsStream(CONFIG_TEMPLATE)) {
            if (in == null) {
                log.error("未找到内置模板 {}", CONFIG_TEMPLATE);
                return;
            }
            Files.copy(in, config, StandardCopyOption.REPLACE_EXISTING);
            log.info("已创建 {}，请填写配置后重新启动。", CONFIG_FILE);
        } catch (IOException e) {
            log.error("无法创建 {}：{}", CONFIG_FILE, e.getMessage());
        }
    }
}

package JOO.jooshop.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration // spring 설정 클래스임을 명시
public class WebConfig implements WebMvcConfigurer {

    /**
     * 배포용 코드
     @Value("${file.upload-dir:uploads/}")  // application.yml에서 경로 주입
     private String uploadDir;

     @Override
     public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
     }

     application.yml
     file:
        upload-dir: /home/ec2-user/app/uploads
     */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // src/main/resources/static/uploads/thumbnails/ 에 저장하면 JAR 빌드 후, 변경사항 반영 안 됨
        // 클래스패스(static 디렉토리)는 읽기 전용이다. 이미지를 resources/static 에 저장하는 건 개발일떄, 만 가능한 트릭이다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/");
    }
}


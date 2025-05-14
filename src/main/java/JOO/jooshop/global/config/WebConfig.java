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
     * Adds a resource handler to the provided registry.
     * This method maps the "/uploads/**" path pattern to the "file:src/main/resources/static/uploads/" location,
     * allowing these resources to be served by the web server.
     *
     * /uploads/** 경로로 들어오는 요청을 로컬 디스크의 static/uploads/ 디렉토리에 있는 파일로 매핑하여 제공하는 역할을 합니다.
     *
     * @param registry the registry to which the resource handler is added
     */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // src/main/resources/static/uploads/thumbnails/ 에 저장하면 JAR 빌드 후, 변경사항 반영 안 됨
        // 클래스패스(static 디렉토리)는 읽기 전용이다. 이미지를 resources/static 에 저장하는 건 개발일떄, 만 가능한 트릭이다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/");
    }
}


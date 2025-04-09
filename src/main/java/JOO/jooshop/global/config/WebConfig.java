package JOO.jooshop.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
        // "/uploads/**" 경로로 시작하는 요청이 "src/main/resources/static/uploads/" 디렉토리에서 정적 파일을 제공할 수 있다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/");
    }
}

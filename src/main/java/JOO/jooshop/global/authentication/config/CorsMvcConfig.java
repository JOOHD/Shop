package JOO.jooshop.global.authentication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    /**
     * addCorsMappings 는 왜 JWT + 쿠키에 필요할까?
     * .exposeHeaders("Set-Cookie") 브라우저가 응답의 Set-Cookie 를 인식
     * .allowCredentials(true) 브라우저가 쿠키를 요청과 함께 자동으로 전송할 수 있게 함
     * .allowedOrigins(frontendUrl) 크로스 도메인 요청 허용, 하지만 정확한 도메인만 허용해야 보안이 올라감
     *
     * CORS 설정에서 exposedHeaders("Set-Cookie") 없으면 브라우저가 이 헤더를 무시함
     * allowCredentials(true) 없으면 브라우저가 쿠키를 서버로 보낼 수 없음 → JWT 인증 불가
     */
    @Value("${spring.frontend.url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        corsRegistry.addMapping("/**") // 모든 경로에 대해 CORS 설정 적용
                .exposedHeaders("Set-Cookie")    // 응답 헤더 cookie 를 프론트에서 읽을 수 있도록 허용
                .allowedOrigins(frontendUrl)     // 접근 허용할 프론트 주소
                .allowCredentials(true);         // 쿠키 포함 요청 허용
    }
}

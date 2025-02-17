package JOO.jooshop.global.config;

import JOO.jooshop.global.jwts.service.CookieService;
import JOO.jooshop.global.jwts.utils.JWTUtil;
import JOO.jooshop.global.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // [LoginFilter] Bean 등록

    private final ObjectMapper objectMapper;

    private final JWTUtil jwtUtil;

    private final CookieService cookieService;

    private final RefreshRepository refreshRepository;
    // [MemberService] Bean 등록

    private final MemberRepositoryV1 memberRepository;
    // [Social 로그인] 을 위한 생성자 주입

    private final CustomOAuth2UserServiceV1 customOAuth2UserService;

    private final CustomLoginSuccessHandlerV1 customLoginSuccessHandler;

    private final CustomLoginFailureHandler customLoginFailureHandler;

    @Value("${frontend.url}")
    private String frontendUrl;
}

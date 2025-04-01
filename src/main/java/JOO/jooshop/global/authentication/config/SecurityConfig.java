package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.jwts.filters.CustomJsonEmailPasswordAuthenticationFilter;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.service.CookieService;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV1;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

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

    @Bean
    @Primary
    public AuthenticationConfiguration authenticationConfiguration() {
        return new AuthenticationConfiguration();
    }

    @Bean
    public LoginFilter loginFilter() throws Exception {
        // LoginFilter
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return customLoginSuccessHandler;
    }
    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return customLoginFailureHandler;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepositoryV1, passwordEncoder());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CustomJsonEmailPasswordAuthenticationFilter customJsonUsernamePasswordAuthenticationFilter() throws Exception {
        return new CustomJsonEmailPasswordAuthenticationFilter(authenticationManager(authenticationConfiguration()), objectMapper);
    }
}

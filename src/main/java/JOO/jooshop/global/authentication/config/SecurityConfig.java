package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.jwts.filters.CustomJsonEmailPasswordAuthenticationFilter;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV1;
import JOO.jooshop.global.authorization.CustomAuthorizationRequestResolver;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // [LoginFilter] Bean 등록
    private final ObjectMapper objectMapper;

    private final JWTUtil jwtUtil;

    private final RedisTemplate<String, String> redisTemplate;

    private final RefreshRepository refreshRepository;

    private final MemberRepositoryV1 memberRepository;
    // [Social 로그인] 을 위한 생성자 주입
    private final ClientRegistrationRepository clientRegistrationRepository;

    private final CustomOAuth2UserServiceV1 customOAuth2UserService;

    private final CustomLoginSuccessHandlerV1 customLoginSuccessHandler;

    private final CustomLoginFailureHandler customLoginFailureHandler;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    @Primary // 인증 관련 설정을 제공하는 객체
    public AuthenticationConfiguration authenticationConfiguration() {
        return new AuthenticationConfiguration();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/css/**", "/js/**");
    }

    @Bean
    public LoginFilter loginFilter() throws Exception {
        // LoginFilter 에 생각보다 필요한 dependency 가 많아서, Bean 으로 따로 관리
        return new LoginFilter(
                // 인증 수행
                authenticationManager(authenticationConfiguration()),
                objectMapper,       // JSON 파싱
                memberService(),    // 회원 정보 조회
                jwtUtil,            // JWT 생성 및 검증
                refreshRepository   // refreshToken 저장소
        );
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
        return new MemberService(memberRepository, passwordEncoder());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CustomJsonEmailPasswordAuthenticationFilter customJsonUsernamePasswordAuthenticationFilter() throws Exception {
        return new CustomJsonEmailPasswordAuthenticationFilter(authenticationManager(authenticationConfiguration()), objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CORS 설정 - 프론트엔드 도메인에서 요청 허용
        http.cors(corsCustomizer -> corsCustomizer.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(Collections.singletonList(frontendUrl));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowCredentials(true);
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setMaxAge(3600L);
            config.addExposedHeader("Set-Cookie");
            config.addExposedHeader("Authorization");
            return config;
        }));

        // REST API 특성상 CSRF, 폼 로그인, HTTP Basic 인증 비활성화
        http.csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        
        // formLogin 활성화 & 로그인 페이지 지정
        http.formLogin(form -> form
                .loginPage("/login")          // 로그인 화면 경로 지정 (GET / login 요청 시 뷰 보여줌)
                .loginProcessingUrl("/login") // 로그인 POST 처리 URL
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()                  // 로그인 페이지는 인증 없이 접근 허용
        );

        // 기본 로그아웃 기능 비활성화 (커스텀 로그아웃 구현 가능)
        http.logout(logout -> logout.disable());

        // 권한 설정
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/logout", "/", "/join", "/auth/**", "/login/oauth2/code/**").permitAll()
                .requestMatchers("/api/v1/categorys/**", "/api/v1/thumbnail/**", "/api/v1/members/**").permitAll()
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/v1/products/**")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/products/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers(antMatcher(HttpMethod.PUT, "/api/v1/products/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/v1/products/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/thumbnail/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers(antMatcher(HttpMethod.PUT, "/api/v1/thumbnail/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/v1/thumbnail/**")).hasAnyRole("ADMIN", "SELLER")
                .requestMatchers("/admin", "/api/v1/inventory/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/reissue/access", "/api/v1/reissue/refresh").permitAll()
                .requestMatchers("/api/v1/inquiry/**").permitAll()
                .requestMatchers("/api/v1/inquiry/reply/**").hasAnyRole("ADMIN", "SELLER")
                .anyRequest().permitAll()
        );

        // JWT 필터 등록 (토큰 검증 → 로그인)
        http
            .addFilterBefore(new JWTFilterV3(jwtUtil, redisTemplate, memberRepository), UsernamePasswordAuthenticationFilter.class);
        http
            .addFilterBefore(loginFilter(), JWTFilterV3.class);

        // OAuth2 로그인 설정 (소셜 로그인 처리)
        http
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(auth -> auth
                            .authorizationRequestResolver(
                                    new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                            )
                    )
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(loginSuccessHandler())
                    .failureHandler(loginFailureHandler())
            );

        // 세션을 서버에서 관리하지 않는 완전 무상태(stateless) 설정
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

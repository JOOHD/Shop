package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.jwts.filters.CustomJsonEmailPasswordAuthenticationFilter;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV1;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

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
        // 소셜 로그인 성공 시, 메인 도메인으로 Redirect 해주기 위해, CorsConfiguration 등록
        http
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {


                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList(frontendUrl));
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        // Expose multiple headers correctly
                        configuration.addExposedHeader("Set-Cookie");
                        configuration.addExposedHeader("Authorization");

                        return configuration;
                    }
                }));

        // csrf disable
        http
                .csrf((auth) -> auth.disable());

        // Form 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        // HTTP Basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        // 기본 로그아웃 비활성화. 기본적으로 등록되어 있던 LogoutFilter 는 비활성화 되고, Logout Api ("/logout") 를 통해 로그아웃이 진행됩니다.
        http
                .logout(logout -> logout.disable());

        /*
        // 경로별 인가 작업
        http.authorizeHttpRequests((auth) -> auth
                // 메인 페이지, 로그인, 회원가입 페이지에 대한 권한 : ALL
                .requestMatchers("/login", "/logout", "/", "/join", "/auth/**", "/login/oauth2/code/**").permitAll()
                // 상품 카테고리, 상품
                .requestMatchers("/api/v1/categorys/**", "/api/v1/thumbnail/**", "/api/v1/members/**").permitAll()
                .requestMatchers(antMatcher(
                        HttpMethod.GET, "/api/v1/products/**")).permitAll()
                .requestMatchers(antMatcher(
                        HttpMethod.POST, "/api/v1/products/**")).hasRole("ADMIN, SELLER")
                .requestMatchers(antMatcher(
                        HttpMethod.PUT, "/api/v1/products/**")).hasRole("ADMIN, SELLER")
                .requestMatchers(antMatcher(
                        HttpMethod.DELETE, "/api/v1/products/**")).hasRole("ADMIN, SELLER")
                // 상품 썸네일 이미지
                .requestMatchers("/api/v1/thumbnail/**").permitAll()
                .requestMatchers(antMatcher(
                        HttpMethod.POST, "/api/v1/thumbnail/**")).hasRole("ADMIN, SELLER")
                .requestMatchers(antMatcher(
                        HttpMethod.PUT, "/api/v1/thumbnail/**")).hasRole("ADMIN, SELLER")
                .requestMatchers(antMatcher(
                        HttpMethod.DELETE, "/api/v1/thumbnail/**")).hasRole("ADMIN, SELLER")
                // 관리자 페이지 권한 : 관리자
                .requestMatchers("/admin", "/api/v1/inventory/**").hasRole("ADMIN")
                // access, refresh token 만료 시, 재발행 : ALL
                .requestMatchers("/api/v1/reissue/access", "/api/v1/reissue/refresh").permitAll()
                // 문의
                .requestMatchers("/api/v1/inquiry/**").permitAll()
                // 문의 답변
                .requestMatchers("/api/v1/inquiry/reply/**").hasRole("ADMIN, SELLER")
                // 나머지 페이지 권한: 로그인 멤버
                .anyRequest().permitAll());
        */
        // 경로별 인가 작업: 모든 요청에 대해 허용
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll());
        
        // Logout Api 를 사용할 것이기에, CustomLogoutFilter 를 사용하지 않을 것임.         
//        http
//                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class);

        /**
         순차적으로 등록할 Filter 들을 등록.
         1. 이메일, 패스워드 기반의 LoginFilter
         2. 프론트엔드 로컬스토리지(엑세스), 스토리지쿠키(리프레쉬) 기반의 JWTFilter 를 등록.
         3. CustomLogoutFilter 에서는
         */
        http
                .addFilterBefore(new JWTFilterV3(jwtUtil, redisTemplate, memberRepository), UsernamePasswordAuthenticationFilter.class);
        http
                .addFilterBefore(loginFilter(), JWTFilterV3.class);

        /**
         * CustomOAuth2Login 이후 , 클라이언트에 access, refresh 토큰을 전달할 방법을 잘 몰라서 주석처리.
         * oauth2 에서 우리가 원하는 customOAuth2UserService 를 등록. 카카오/네이버/구글 등재
         */
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint(
                                (userInfoEndpointConfig -> userInfoEndpointConfig
                                        .userService(customOAuth2UserService)
                                )
                        )
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                );

        /*
          세션 설정 : STATELESS .
          */
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

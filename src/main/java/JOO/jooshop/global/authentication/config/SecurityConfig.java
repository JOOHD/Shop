package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.factory.FilterFactory;
import JOO.jooshop.global.authentication.jwts.filters.CustomJsonEmailPasswordAuthenticationFilter;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV2;
import JOO.jooshop.global.authorization.CustomAuthorizationRequestResolver;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

/**
 * SecurityConfig
 *
 * Spring Security 설정을 구성하는 핵심 클래스입니다.
 *
 * ✔ API 경로(/api/**)와 웹 경로(/)** 를 분리하여 각각 SecurityFilterChain을 적용합니다.
 * ✔ 필터(LoginFilter, JWTFilterV3)는 Spring Bean으로 등록하지 않고 수동 생성하여 순환 참조를 방지합니다.
 * ✔ 필터 생성은 별도의 FilterFactory를 통해 수행되며, MemberService 등 필요한 의존성을 생성 시점에 직접 주입합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 1. API용 SecurityFilterChain (JWT 기반 인증)
     *
     * - Stateless 정책
     * - JWTFilter → LoginFilter 순서로 필터 적용
     * - 인증되지 않은 요청은 JWTFilter에서 차단
     * - 로그인 요청(/api/login)은 LoginFilter에서 처리
     */

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;

    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final CustomLoginSuccessHandlerV2 customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler;
    private final FilterFactory filterFactory;

    @Value("${spring.frontend.url}")
    private String frontendUrl;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/css/**", "/js/**");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
    public CustomJsonEmailPasswordAuthenticationFilter customJsonUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        return new CustomJsonEmailPasswordAuthenticationFilter(authenticationManager, objectMapper);
    }

    /**
     * 2. Web용 SecurityFilterChain (Form Login + OAuth2)
     *
     * - CSRF 쿠키 설정 적용
     * - OAuth2 로그인 및 일반 Form 로그인 지원
     * - 세션 정책은 IF_REQUIRED (OAuth2 및 일반 인증 시 세션 유지 필요)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager authenticationManager,
                                                      FilterFactory filterFactory,
                                                      MemberService memberService) throws Exception {

        // 의존성 순환 없이 안전하게 필터 생성
        LoginFilter loginFilter = filterFactory.createLoginFilter(authenticationManager, memberService);
        JWTFilterV3 jwtFilter = filterFactory.createJWTFilter(memberService);

        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/join", "/api/admin/join",
                                "/api/verify",
                                "/api/email/verify",
                                "/api/email/verify-request",
                                "/api/email/verify-check"
                        ).permitAll()
                        .requestMatchers("/api/v1/categorys/**", "/api/v1/thumbnail/**", "/api/v1/members/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/profile/**").hasAnyRole("USER", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers("/api/v1/inquiry/reply/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers("/api/v1/reissue/access", "/api/v1/reissue/refresh").permitAll()
                        .requestMatchers("/api/v1/inquiry/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginFilter, JWTFilterV3.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Collections.singletonList(frontendUrl));
                    config.setAllowedMethods(Collections.singletonList("*"));
                    config.setAllowedHeaders(Collections.singletonList("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    config.addExposedHeader("Set-Cookie");
                    config.addExposedHeader("Authorization");
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/", "/auth/**", "/oauth2/**").permitAll()
                        .requestMatchers("/admin", "/api/v1/inventory/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                                )
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(customLoginSuccessHandler)
                        .failureHandler(customLoginFailureHandler)
                )
                .logout(logout -> logout.disable());

        return http.build();
    }
}

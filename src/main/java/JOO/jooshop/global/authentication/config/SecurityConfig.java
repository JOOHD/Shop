package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.factory.FilterFactory;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginFailureHandler;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginSuccessHandler;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginSuccessHandlerV2;
import JOO.jooshop.global.authorization.CustomAuthorizationRequestResolver;
import JOO.jooshop.members.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    private final FilterFactory filterFactory;

    // redisTemplate 충돌 방지 → 내가 만든 Bean 사용
    private final @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate;

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;
    private final Oauth2LoginSuccessHandlerV2 oauth2LoginSuccessHandler;
    private final Oauth2LoginFailureHandler oauth2LoginFailureHandler;

    @Value("${spring.frontend.url}")
    private String frontendUrl;

    // 권한별 URL 그룹핑
    private static final String[] PUBLIC_API = {
            "/api/join",
            "/api/admin/join",
            "/api/verify",
            "/api/email/**",
            "/api/v1/categorys/**",
            "/api/v1/thumbnail/**",
            "/api/v1/members/join",
            "/api/v1/members/check-email",
            "/api/v1/reissue/**",
            "/api/v1/inquiry/**"
    };

    private static final String[] ROLE_USER_OR_SELLER = {
            "/api/v1/profile/**",
            "/api/v1/cart/**",
            "/api/v1/order/**",
            "/api/v1/product/**",
            "/api/v1/payment/**"
    };

    private static final String[] ROLE_ADMIN = {
            "/api/v1/admin/products/**",
            "/api/v1/admin/orders/**",
            "/api/v1/admin/members/**",
            "admin/members/**",
            "/api/v1/inventory/**",
            "/api/v1/inquiry/reply/**"
    };

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

    /** ================== 1) API Security Filter Chain ==================
     * API 요청 (/api/**) → JWT 기반 인증/인가 처리
     *
     * 특징:
     *  - CSRF 비활성화 (JWT 환경)
     *  - 세션 비활성화 (STATELESS)
     *  - CORS: frontendUrl 기반 허용
     *  - FormLogin / HttpBasic 비활성화 (오직 JWT만 사용)
     *
     * 권한:
     *  - PUBLIC_API → permitAll()
     *  - 상품 조회(GET) → permitAll()
     *  - USER / SELLER 전용 API → hasAnyRole("USER", "SELLER")
     *  - ADMIN 전용 API → hasRole("ADMIN")
     *  - 그 외 모든 요청은 인증 필요
     *
     * 예외 처리:
     *  - 인증 실패 → 401 JSON 응답
     *  - 권한 부족 → 403 JSON 응답
     *
     * 필터:
     *  - JWTFilterV3: 토큰 유효성 검사 + SecurityContext 저장
     *  - LoginFilter: 로그인 성공 시 JWT 발급
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager authenticationManager,
                                                      MemberService memberService) throws Exception {

        JWTFilterV3 jwtFilter = new JWTFilterV3(jwtUtil, redisTemplate, memberService);
        var loginFilter = filterFactory.createLoginFilter(authenticationManager, memberService);

        http
                .securityMatcher("/api/**")
                // CSRF 불필요 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)
                // JWT 기반이므로 세션을 STATELESS 모드로 유지
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 브라우저에서 오는 요청을 위해 CORS 허용
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Collections.singletonList(frontendUrl));
                    config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
                    config.setAllowedHeaders(Collections.singletonList("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    config.addExposedHeader("Set-Cookie");
                    config.addExposedHeader("Authorization");
                    return config;
                }))
                // 기본 로그인 방식 비활성화 (JWT만 허용)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 인가 정책
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, ROLE_USER_OR_SELLER).hasAnyRole("USER", "SELLER")
                        .requestMatchers(HttpMethod.PUT, ROLE_USER_OR_SELLER).hasAnyRole("USER", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, ROLE_USER_OR_SELLER).hasAnyRole("USER", "SELLER")
                        .requestMatchers(HttpMethod.POST, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ROLE_ADMIN).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 302 리다이렉트 대신 JSON 응답 반환
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"message\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"message\":\"Forbidden\"}");
                        })
                );

        http.addFilterBefore(loginFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** ================== 2) Web Security Filter Chain ==================
     * 일반 웹 요청 (브라우저) → 세션 + 폼 로그인 + OAuth2 로그인
     *
     * 특징:
     *  - CSRF 활성화 (CookieCsrfTokenRepository)
     *  - 세션: IF_REQUIRED (필요할 때만 생성)
     *  - FormLogin 활성화 → 커스텀 성공/실패 핸들러
     *  - OAuth2 로그인 활성화 → CustomOAuth2UserService 사용
     *
     * 권한:
     *  - /login, /formLogin, /logout, /, /auth/**, /products/** → permitAll()
     *  - /profile → 인증 필요
     *  - /admin → ADMIN 권한 필요
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, MemberService memberService) throws Exception {
        var jwtFilterV3 = filterFactory.createJWTFilter(memberService);

        http
                .securityMatcher("/**")
                // Web에서는 CSRF 활성화 (단, /api/**는 제외)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Collections.singletonList(frontendUrl));
                    config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
                    config.setAllowedHeaders(Collections.singletonList("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    config.addExposedHeader("Set-Cookie");
                    config.addExposedHeader("Authorization");
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/formLogin", "/logout", "/", "/auth/**", "/products/**").permitAll()
                        .requestMatchers("/profile").authenticated()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                // FormLogin 활성화
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/formLogin")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler)
                        .permitAll()
                )
                // OAuth2 로그인 활성화
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(authz -> authz
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                                )
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauth2LoginSuccessHandler)
                        .failureHandler(oauth2LoginFailureHandler)
                )
                .logout(logout -> logout.disable());

        http.addFilterBefore(jwtFilterV3, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

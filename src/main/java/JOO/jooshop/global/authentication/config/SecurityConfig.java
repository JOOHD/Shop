package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.factory.FilterFactory;
import JOO.jooshop.global.authentication.jwts.filters.CustomLogoutFilter;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginFailureHandler;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginSuccessHandler;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginSuccessHandlerV2;
import JOO.jooshop.global.authorization.CustomAuthorizationRequestResolver;
import JOO.jooshop.members.repository.RefreshRepository;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
    private final @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;
    private final Oauth2LoginSuccessHandlerV2 oauth2LoginSuccessHandler;
    private final Oauth2LoginFailureHandler oauth2LoginFailureHandler;

    @Value("${spring.frontend.url}")
    private String frontendUrl;

    // API 접근 권한 그룹
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
            "/admin/members/**",
            "/api/v1/inventory/**",
            "/api/v1/inquiry/reply/**"
    };

    /** AuthenticationManager Bean */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** 정적 리소스(Web) 접근 제외 */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/css/**", "/js/**");
    }

    /** 비밀번호 암호화 */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** ================== 1) API Security Filter Chain ==================
     * JWT 기반 API 인증/인가 처리
     * 특징:
     * - CSRF 비활성화, 세션 Stateless
     * - CORS: frontendUrl 허용
     * - JWTFilterV3: 토큰 검증 후 SecurityContext 저장
     * - LoginFilter: 로그인 시 JWT 발급
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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
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

        // 필터 등록 순서: LoginFilter → JWTFilter
        http.addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** ================== 2) Web Security Filter Chain ==================
     * 브라우저 요청(Web) 인증/인가 처리
     * 특징:
     * - CSRF 활성화 (단, /api/** 제외)
     * - 세션: 필요 시 생성
     * - FormLogin + OAuth2 로그인
     * - CustomLogoutFilter: 로그아웃 처리 → JWT Refresh 제거, 블랙리스트 등록
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, MemberService memberService, RefreshRepository refreshRepository) throws Exception {

        JWTFilterV3 jwtFilterV3 = filterFactory.createJWTFilter(memberService);
        CustomLogoutFilter customLogoutFilter = new CustomLogoutFilter(jwtUtil, redisTemplate, refreshRepository);

        http
                .securityMatcher("/**")
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
                // Spring Security 기본 logout 사용
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/") // 로그아웃 후 이동할 페이지
                        .permitAll());

        // 필터 등록 순서: JWTFilter → CustomLogoutFilter
        http.addFilterBefore(jwtFilterV3, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(customLogoutFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

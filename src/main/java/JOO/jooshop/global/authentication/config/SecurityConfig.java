package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.factory.FilterFactory;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginFailureHandler;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginSuccessHandler;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.Oauth2LoginSuccessHandlerV2;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
      클라이언트 요청
         ↓
      SecurityFilterChain (HttpSecurity 설정)
         ↓
      JWTFilterV3 (우리가 추가한 커스텀 필터)
         ↓
         - Authorization 헤더 확인
         - JWT 유효성 검증
         - 성공 → SecurityContext에 Authentication 저장
         - 실패 → 401 Unauthorized 반환
         ↓
      (필요 시) UsernamePasswordAuthenticationFilter (폼 로그인)
         ↓
      AuthenticationManager → Provider (DaoAuthenticationProvider 등)
         ↓
      UserDetailsService (DB 사용자 조회) + PasswordEncoder 검증
         ↓
      성공 시 SecurityContext에 Authentication 저장
         ↓
      Controller(@RestController / @Controller)
     */

    private final JWTUtil jwtUtil;
    private final RedisTemplate redisTemplate;
    private final FilterFactory filterFactory;

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
            "/api/v1/members/**",
            "/api/v1/reissue/**",
            "/api/v1/inquiry/**"
    };

    private static final String[] ROLE_USER_OR_SELLER = {
            "/api/v1/profile/**",
            "/api/v1/cart/**",
            "/api/v1/order/**"
    };

    private static final String[] ROLE_ADMIN = {
            "/api/v1/products/**",
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
     API Security (JWT only)
     매칭 경로: /api/**
     특징:
        - csrf().disable() (REST API 특성)
        - sessionCreationPolicy(STATELESS) (세션 저장 안 함)
        - cors() → frontendUrl 기반 CORS 허용
        - formLogin().disable() + httpBasic().disable() (중요!)

     인증 실패 시: JSON 응답 반환
        - 401 Unauthorized → { "message": "Unauthorized" }
        - 403 Forbidden → { "message": "Forbidden" }

     필터:
        - JWTFilterV3 (쿠키+헤더 토큰 읽고 블랙리스트/만료 체크, SecurityContext 저장)
        - LoginFilter (로그인 후 JWT 발급)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager authenticationManager,
                                                      MemberService memberService) throws Exception {

        // 개선된 JWTFilterV3 사용
        JWTFilterV3 jwtFilter = new JWTFilterV3(jwtUtil, redisTemplate, memberService);

        // LoginFilter (폼 로그인 → JWT 발급)
        var loginFilter = filterFactory.createLoginFilter(authenticationManager, memberService);

        http
                .securityMatcher("/api/**") // API 요청만 매칭
                .csrf(csrf -> csrf.disable()) // JWT라서 CSRF 불필요
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
                // 세션 끊기
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // FormLogin/HTTP Basic 비활성화 -> DaoAuthenticationProvider 개입 차단
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.PUT, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.DELETE, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.POST, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ROLE_ADMIN).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                //  API는 302 리다이렉트 금지 -> JSON 으로 401/403 반환
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

        // [유지] 커스텀 필터 체인
        http.addFilterBefore(loginFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        //  API 체인에서는 oauth2Login을 제거 (리다이렉트 유발 원인)
        // .oauth2Login(...) 사용 금지

        return http.build();
    }

    /** ================== 2) Web Security Filter Chain ==================
     Web Security (폼 로그인 + OAuth2)
     매칭 경로: /** (API 제외 나머지)
     특징:
        - csrf(CookieCsrfTokenRepository) 활성화 (브라우저 기반 폼 전송 보호)
        - 세션 정책: IF_REQUIRED (필요할 때만 세션 생성)
        - formLogin() 활성화
            - /login 페이지
            - /formLogin 엔드포인트
            - 로그인 성공/실패 핸들러 커스터마이징

        - oauth2Login() 활성화
            - CustomOAuth2UserService 로 사용자 정보 매핑
            - 성공/실패 핸들러 커스터마이징

     필터:
        - JWTFilterV3 (세션 기반과 공존 가능)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, MemberService memberService) throws Exception {
        var jwtFilterV3 = filterFactory.createJWTFilter(memberService);

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
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/formLogin")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2   //  OAuth2는 Web 체인에만 둠
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
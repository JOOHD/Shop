package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.jwts.filters.CustomJsonEmailPasswordAuthenticationFilter;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginFailureHandler;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginSuccessHandler;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.service.CustomOAuth2UserServiceV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginFailureHandler;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV1;
import JOO.jooshop.global.authentication.oauth2.handler.CustomLoginSuccessHandlerV2;
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
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final RefreshRepository refreshRepository;
    private final MemberRepositoryV1 memberRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;

    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;
    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final CustomLoginSuccessHandlerV2 customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler;

    @Value("${frontend.url}")
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
    public MemberService memberService() {
        return new MemberService(memberRepository, passwordEncoder());
    }

    @Bean
    public LoginFilter loginFilter(AuthenticationManager authenticationManager) {
        LoginFilter loginFilter = new LoginFilter(
                authenticationManager,
                objectMapper,
                memberService(),
                jwtUtil,
                refreshRepository
        );
        loginFilter.setFilterProcessesUrl("/api/login"); // JSON 로그인용 경로 설정
        return loginFilter;
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

    @Bean
    @Order(1) // API 먼저 처리
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/api/**")    // API 경로만 적용
                .csrf(csrf -> csrf.disable())  // API 요청은 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ STATELESS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/join", "/api/admin/join").permitAll()
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
                .addFilterBefore(new JWTFilterV3(jwtUtil, redisTemplate, memberRepository), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginFilter(authenticationManager), JWTFilterV3.class);

        return http.build();
    }

    @Bean
    @Order(2) // 그 외 모든 요청 (Form Login, OAuth2 등)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // API 경로는 CSRF 무시 (안 해도 위에서 이미 처리됨)
                        .csrfTokenRepository(CookieCsrfTok enRepository.withHttpOnlyFalse()) // 폼 로그인용 CSRF 활성화 및 쿠키 방식 저장
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // ✅ 세션 사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/", "/auth/**", "/oauth2/**").permitAll()
                        .requestMatchers("/admin", "/api/v1/inventory/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler)
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
                .logout(logout -> logout.disable()); // 커스텀 로그아웃 필터 있는 경우

        return http.build();
    }
}

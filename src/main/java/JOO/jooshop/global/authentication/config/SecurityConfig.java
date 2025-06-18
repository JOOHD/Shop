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

    private final ObjectMapper objectMapper;
    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final RefreshRepository refreshRepository;
    private final MemberRepositoryV1 memberRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final CustomLoginSuccessHandlerV1 customLoginSuccessHandler;
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
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        // CORS
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(Collections.singletonList(frontendUrl));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            config.addExposedHeader("Set-Cookie");
            config.addExposedHeader("Authorization");
            return config;
        }));

        // 인가 설정
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/logout", "/", "/auth/**", "/login/oauth2/code/**", "/api/join", "/api/admin/join").permitAll()
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
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
        );

        // CSRF 설정: REST API는 비활성화
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        // Form 로그인 설정 (웹 UI)
        http.formLogin(form -> form
                .loginPage("/login")                // 로그인 form page
                .loginProcessingUrl("/api/login")   // 로그읜 처리 요청 URL
                .defaultSuccessUrl("/")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
        );

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(auth -> auth
                        .authorizationRequestResolver(
                                new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                        )
                )
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
        );

        // 세션 설정: Form 로그인용은 stateful, API는 stateless
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 로그아웃 비활성화 (커스텀 필터 사용 시)
        http.logout(logout -> logout.disable());

        // JWT 필터 등록
        http.addFilterBefore(new JWTFilterV3(jwtUtil, redisTemplate, memberRepository), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(loginFilter(authenticationManager), JWTFilterV3.class);

        return http.build();
    }
}

package JOO.jooshop.global.authentication.config;

import JOO.jooshop.global.authentication.factory.FilterFactory;
import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginFailureHandler;
import JOO.jooshop.global.authentication.jwts.handler.FormLoginSuccessHandler;
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

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomOAuth2UserServiceV1 customOAuth2UserService;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;
    private final Oauth2LoginSuccessHandlerV2 oauth2LoginSuccessHandler;
    private final Oauth2LoginFailureHandler oauth2LoginFailureHandler;
    private final FilterFactory filterFactory;

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

    // ================== 1) API Security Filter Chain ==================
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager authenticationManager,
                                                      MemberService memberService) throws Exception {

        var loginFilter = filterFactory.createLoginFilter(authenticationManager, memberService);
        var jwtFilter = filterFactory.createJWTFilter(memberService);

        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
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
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        // [참고] 카트 GET도 인증 필요 (서버에서 사용자 식별)
                        .requestMatchers(HttpMethod.POST, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.PUT, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.DELETE, ROLE_USER_OR_SELLER).hasAnyRole("USER","SELLER")
                        .requestMatchers(HttpMethod.POST, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ROLE_ADMIN).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ROLE_ADMIN).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                //  API 체인에서는 로그인/리다이렉트를 제거하고 401로 응답
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

    // ================== 2) Web Security Filter Chain ==================
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
package JOO.jooshop.global.authentication.jwts.handler;

import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class FormLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Value("${spring.backend.url}")
    private String backendUrl;

    @Value("${app.secure}")
    private boolean isSecure;

    public FormLoginSuccessHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("폼 로그인 성공, JWT 토큰 생성 및 응답");

        String userId = String.valueOf(authentication.getName());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("USER");

        String accessToken = jwtUtil.createAccessToken("access", userId, role);
        String refreshToken = jwtUtil.createRefreshToken("refresh", userId, role);

        log.info("발급된 AccessToken: {}", accessToken);

        if (isSecure) {
            CookieUtil.createCookieWithSameSite(response, "accessToken", accessToken, 900);
            CookieUtil.createCookieWithSameSite(response, "refreshAuthorization", refreshToken, 1209600);
        } else {
            CookieUtil.createCookieWithSameSiteForLocal(response, "accessToken", accessToken, 900);
            CookieUtil.createCookieWithSameSiteForLocal(response, "refreshAuthorization", refreshToken, 1209600);
        }

        // 로그인 성공 후 홈으로 이동
        getRedirectStrategy().sendRedirect(request, response, backendUrl + "/");
    }
}

package JOO.jooshop.global.authentication.jwts.handler;

import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("폼 로그인 성공, JWT 토큰 생성 및 응답");

        String userId = String.valueOf(authentication.getName()); // 보통 username
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_USER");

        String accessToken = jwtUtil.createAccessToken("access", userId, role);
        String refreshToken = jwtUtil.createRefreshToken("refresh", userId, role);

        // 쿠키 세팅 (SameSite 옵션 포함)
        CookieUtil.createCookieWithSameSite(response, "accessToken", accessToken, 900);         // 15분
        CookieUtil.createCookieWithSameSite(response, "refreshToken", refreshToken, 1209600);  // 14일

        // JSON 응답 (optional, 필요 시)
        /*
            JsonObject responseData = new JsonObject();
            responseData.addProperty("accessToken", accessToken);
            responseData.addProperty("refreshToken", refreshToken);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseData.toString());
        */

        // 로그인 성공 후 리다이렉트 (프로필 페이지 등)
        response.setStatus(HttpStatus.OK.value());
        getRedirectStrategy().sendRedirect(request, response, "/profile");  // 원하는 URL로 변경 가능
    }
}

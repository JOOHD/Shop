package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.members.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Date;

@RequiredArgsConstructor
@Slf4j
public class CustomLogoutFilter extends GenericFilterBean {

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RefreshRepository refreshRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, filterChain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // 1.  /logout 요청을 받아, 쿠키에서 Refresh Token 제거
        // 2.  DB의 Refresh Token 레코드를 삭제
        // 3.  응답 헤더에서 Authorization 제거
        // 4.  Access Token을 블랙리스트에 등록

        //path and method verify
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^/logout$") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Access Token 블랙리스트 처리
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            try {
                String tokenId = jwtUtil.getId(accessToken);
                Date expiration = jwtUtil.getExpiration(accessToken); // 만료 시간 (ms 단위)
                long currentTime = System.currentTimeMillis();        // 현재 시간
                long remainTime = (expiration.getTime() - currentTime) / 1000; // 남은 시간 (초)

                // Redis 블랙리스트에 저장
                redisTemplate.opsForValue().set("blacklist:" + accessToken, "logout", remainTime);
            } catch (Exception e) {
                log.warn("블랙리스트 등록 실패: {}", e.getMessage());
            }
        }

        // 2. Refresh Token 삭제
        Cookie[] cookies = request.getCookies();
        String refresh = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refresh = cookie.getValue();
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }

        if (refresh != null && !jwtUtil.isExpired(refresh) && "refresh".equals(jwtUtil.getCategory(refresh))) {
            if (refreshRepository.existsByRefreshToken(refresh)) {
                refreshRepository.deleteByRefreshToken(refresh);
            }
        }

        // 응답
        response.setHeader("Authorization", "");
        response.setStatus(HttpServletResponse.SC_OK);
        log.info("로그아웃 완료. 토큰 블랙리스트 등록 및 Refresh 제거.");
        response.getWriter().write("로그아웃에 성공했습니다.");
    }
}

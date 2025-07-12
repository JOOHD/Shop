package JOO.jooshop.global.authentication.jwts.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Optional;

public class TokenResolver {

    public static Optional<String> resolveTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ?
                Optional.of(header.substring(7)) : Optional.empty();
    }

    public static Optional<String> resolveTokenFromCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
//                보통 쿠키에 저장한는 JWT는 "Bearer+" 접두사 없이 순수 토큰만 넣는다.
//                .filter(v -> v.startsWith("Bearer+"))
//                .map(v -> v.substring(7));
    }
}

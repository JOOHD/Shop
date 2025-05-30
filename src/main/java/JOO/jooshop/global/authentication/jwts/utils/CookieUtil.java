package JOO.jooshop.global.authentication.jwts.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CookieUtil {

    public static Cookie createCookie(String key, String value, int maxAgeInSeconds) {
        Cookie cookie = new Cookie(key, value);
        // 만료 기간을 설정하지 않음으로, 세션 쿠키를 사용(브라우저 종료 시 삭제). 퍼시스턴트 쿠키를 사용 안함(설정한 시간 동안 유지)
        cookie.setMaxAge(maxAgeInSeconds);
        cookie.setSecure(true);        // HTTPS 에서만 전송
        cookie.setPath("/");           // 모든 경로에서 사용가능
        cookie.setHttpOnly(true);      // Javascript 접근 차단 (보안 강화)
        return cookie;
    }

    public static void createCookieWithSameSite(HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String cookieString = String.format(
               "%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
               name, encodedValue,  maxAgeInSeconds
       );
       response.addHeader("Set-Cookie", cookieString);
    }

    // 쿠키 가져오기, 쿠키는 일반적으로 HttpServletRequest에서 가져온다.
    public static String getCookieValue(HttpServletRequest request,  String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    try {
                        // 쿠키는 + 로 인코딩되기 때문에 공백 처리 필요
                        return URLDecoder.decode(cookie.getValue(), "UTF-8").replace("+", " ");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // 쿠키 삭제
    public static void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

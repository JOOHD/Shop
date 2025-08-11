package JOO.jooshop.global.authentication.jwts.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CookieUtil {

    // 프로덕션(HTTPS) 환경용 쿠키 생성
    public static Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        // 만료 기간을 설정하지 않음으로, 세션 쿠키를 사용(브라우저 종료 시 삭제). 퍼시스턴트 쿠키를 사용 안함(설정한 시간 동안 유지)
        cookie.setMaxAge(60*60*60);
        cookie.setSecure(true);        // HTTPS 에서만 전송 (true), but 로컬 테스트용으로 (false) 설정
        cookie.setPath("/");            // 모든 경로에서 사용가능
        cookie.setHttpOnly(true);       // Javascript 접근 차단 (보안 강화)
        return cookie;
    }

    // 로컬(HTTP) 환경용 쿠키 생성 (Secure=false)
    public static Cookie createCookieForLocal(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60 * 60 * 60);
        cookie.setSecure(false);       // HTTP 환경에서 테스트용
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    // 프로덕션(HTTPS) 환경용 SameSite=None 쿠키 생성
    public static void createCookieWithSameSite(HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String cookieString = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                name, encodedValue, maxAgeInSeconds
        );
        response.addHeader("Set-Cookie", cookieString);
    }

    // 로컬(HTTP) 환경용 SameSite=Lax 쿠키 생성 (Secure 없음)
    public static void createCookieWithSameSiteForLocal(HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String cookieString = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                name, encodedValue, maxAgeInSeconds
        );
        response.addHeader("Set-Cookie", cookieString);
    }

    // 쿠키 값 가져오기
    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8).replace("+", " ");
                }
            }
        }
        return null;
    }

    // 쿠키 삭제 (Secure 옵션 제거하지 않음 — 삭제하려는 쿠키와 동일 옵션으로 삭제해야 함)
    public static void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(false); // 로컬에서는 false, 배포환경에선 true로 바꿔서 삭제 필요
        response.addCookie(cookie);
    }
}

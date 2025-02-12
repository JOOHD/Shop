package JOO.jooshop.global.jwts.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        // 만료 기간을 설정하지 않음으로, 세션 쿠키를 사용(브라우저 종료 시 삭제). 퍼시스턴트 쿠키를 사용 안함(설정한 시간 동안 유지)
        // cookie.setMaxAge(60*60*60); // 7일 유지
        // cookie.setSecure(true);     // HTTPS 에서만 전송
        cookie.setPath("/");           // 모든 경로에서 사용가능
        cookie.setHttpOnly(true);      // Javascript 접근 차단 (보안 강화)

        return cookie;
    }

    // 쿠키 가져오기, 쿠키는 일반적으로 HttpServletRequest에서 가져온다.
    public static String getCookieValue(HttpServletRequest request,  String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
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

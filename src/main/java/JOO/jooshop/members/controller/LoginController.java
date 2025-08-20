package JOO.jooshop.members.controller;

import JOO.jooshop.global.Exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.Exception.customException.UnverifiedEmailException;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final MemberService memberService;
    private final JWTUtil jwtUtil;

    // 로그인 페이지 GET
    @GetMapping("/login")
    public String formLogin(Authentication authentication) {
        // 이미 로그인 상태라면 홈으로 리다이렉트
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "members/login";
    }

    // 로그인 상태 확인 (currentUser)
    @GetMapping("/user/me")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> currentUser(@CookieValue(value = "accessToken", required = false) String accessToken) {
        Map<String, Object> res = new HashMap<>();
        boolean loggedIn = false;

        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            loggedIn = true;
        }
        
        res.put("loggedIn", loggedIn);
        return ResponseEntity.ok(res);
    }

    // 로그인 POST (API)
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<String> oauth2Login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            String loginResult = memberService.login(loginRequest, response);
            Cookie jwtCookie = new Cookie("accessToken", loginResult);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");

            // 만료시간 계산 후 쿠키 maxAge 설정
            Date expiration = jwtUtil.getExpiration(loginResult);
            int maxAge = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
            jwtCookie.setMaxAge(maxAge);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok("로그인 성공, 환영합니다.");
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일 또는 비밀번호가 틀렸습니다.");
        } catch (UnverifiedEmailException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("인증되지 않은 이메일입니다.");
        }
    }
}

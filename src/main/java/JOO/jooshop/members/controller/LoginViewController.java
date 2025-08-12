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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequiredArgsConstructor
public class LoginViewController {

    private final MemberService memberService;
    private final JWTUtil jwtUtil;

    @GetMapping("/login")
    public String formLogin() {
        return "members/login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<String> oauth2Login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            String loginResult = memberService.login(loginRequest, response);
            Cookie jwtCookie = new Cookie("accessToken", loginResult);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");

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

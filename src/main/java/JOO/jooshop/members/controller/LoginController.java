package JOO.jooshop.members.controller;

import JOO.jooshop.global.exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.exception.customException.UnverifiedEmailException;
import JOO.jooshop.members.model.request.LoginRequest;
import JOO.jooshop.members.service.MemberAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final MemberAuthService memberAuthService;

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "members/login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<String> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        try {
            memberAuthService.login(loginRequest, response);
            return ResponseEntity.ok("로그인 성공, 환영합니다.");
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("이메일 또는 비밀번호가 틀렸습니다.");
        } catch (UnverifiedEmailException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("인증되지 않은 이메일입니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }
}
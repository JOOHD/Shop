package JOO.jooshop.global.mail.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
public class EmailVerificationController {

    /**
     * CertificationEntity: 토큰 저장 및 만료 관리
     * EmailMemberService: 토큰 생성, 인증 처리, 메일 발송 담당
     * Member: 인증 여부만 관리 (certifiedByEmail)
     * 컨트롤러는 /verify, /resend 기능만 담당
     */
    private final EmailMemberService emailMemberService;
    private final JWTUtil jwtUtil;

    // 이메일 인증 링크 클릭 시 (GET)
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token,
                            HttpServletResponse response,
                            Model model) throws IOException {
        try {
            // 토큰으로 인증 처리 + 인증 완료된 회원 반환
            Member member = emailMemberService.verifyEmailAndReturnMember(token);

            // JWT Access Token 생성
            String accessToken = jwtUtil.createAccessToken(
                    "access_token",
                    String.valueOf(member.getId()),
                    member.getMemberRole().name()
            );

            // 쿠키 설정
            Cookie cookie = new Cookie("access_token", accessToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60); // 초 단위
            response.addCookie(cookie);

            // 이메일 파라미터 Thymeleaf 에 전달
            model.addAttribute("verifiedEmail", member.getEmail());
            return "email/verify-success"; // HTML 내 JS에서 join으로 이동
        } catch (Exception e) {
            return "email/verify-fail";
        }
    }

    // 인증 메일 발송 요청 (POST)
    @PostMapping("/verify-request")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("이메일이 필요합니다.");
        }
        try {
            emailMemberService.sendEmailVerification(email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메일 발송 실패");
        }
    }
}

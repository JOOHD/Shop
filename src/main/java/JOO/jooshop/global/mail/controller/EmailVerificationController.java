package JOO.jooshop.global.mail.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.repository.CertificationRepository;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.CertificationEntity;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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
    private final MemberRepositoryV1 memberRepository;
    private final CertificationRepository certificationRepository;
    private final JWTUtil jwtUtil;

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

    // 이메일 인증 링크 클릭 시 (GET)
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token,
                              HttpServletResponse response,
                              Model model) throws IOException {
        try {
            String email = emailMemberService.verifyEmailAndReturnMember(token);
            model.addAttribute("verifiedEmail", email);

            Optional<Member> memberOpt = memberRepository.findByEmail(email);
            memberOpt.ifPresent(member -> {
                String accessToken = jwtUtil.createAccessToken(
                        "access_token",
                        String.valueOf(member.getId()),
                        member.getMemberRole().name()
                );

                Cookie cookie = new Cookie("access_token", accessToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60);

                response.addCookie(cookie);
            });

            // 회원이 없으면 인증만 완료된 상태 → 회원가입 페이지로 이동 유도 가능
            return "email/verify-success";
        } catch (Exception e) {
            e.printStackTrace();
            return "email/verify-fail";
        }
    }

    // 이메일 인증 상태 확인
    @GetMapping("/verify-check")
    public ResponseEntity<Map<String, Boolean>> verifyCheck(@RequestParam("email") String email) {
        boolean isVerified = certificationRepository.findByEmail(email).isEmpty();

        // 항상 200 OK 응답이고, body가 {"verified":true} 또는 {"verified":false} 형태
        return ResponseEntity.ok(Map.of("verified", isVerified));
    }
}

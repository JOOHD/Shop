package JOO.jooshop.global.mail.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class EmailVerificationController {

    /**
     * CertificationEntity: 토큰 저장 및 만료 관리
     * EmailMemberService: 토큰 생성, 인증 처리, 메일 발송 담당
     * Member: 인증 여부만 관리 (certifiedByEmail)
     * 컨트롤러는 /verify, /resend 기능만 담당
     */
    private final EmailMemberService emailMemberService;
    private final JWTUtil jwtUtil;

    @GetMapping("/verify")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        try {
            // 1. 토큰으로 인증 처리 + 인증 완료된 회원 반환
            Member member = emailMemberService.verifyEmailAndReturnMember(token);

            // 2. JWT Access Token 생성
            String accessToken = jwtUtil.createAccessToken(
                    "access_token",
                    String.valueOf(member.getId()),
                    member.getMemberRole().name()
            );

            // 3. 쿠키 설정
            Cookie accessTokenCookie = new Cookie("access_token", accessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 60); // 초 단위

            response.addCookie(accessTokenCookie);

            // 4. 인증 성공 후 리다이렉트
            response.sendRedirect("/");
        } catch (Exception e) {
            // 인증 실패 시 회원가입 페이지로 이동
            response.sendRedirect("/join");
        }
    }
}

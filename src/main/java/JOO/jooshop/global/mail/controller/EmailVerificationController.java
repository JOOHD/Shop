package JOO.jooshop.global.mail.controller;

import JOO.jooshop.global.mail.service.EmailMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class EmailVerificationController {

    private final EmailMemberService emailMemberService;

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        try {
            emailMemberService.updateByVerifyToken(token);
            model.addAttribute("message", "이메일 인증이 완료되었습니다. 환영합니다!");
        } catch (UsernameNotFoundException e) {
            model.addAttribute("message", "유효하지 않은 인증 링크입니다.");
        }

        return "email/verificationResult";  // ✅ templates/email/verificationResult.html 경로
    }
}
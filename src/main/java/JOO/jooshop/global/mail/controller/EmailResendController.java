package JOO.jooshop.global.mail.controller;

import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
public class EmailResendController {

    private final MemberRepositoryV1 memberRepository;
    private final EmailMemberService emailMemberService;

    @PostMapping("/resend")
    public ResponseEntity<?> resendEmail(@RequestBody Map<String, String> body) throws Exception {
        String email = body.get("email");

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 회원이 존재하지 않습니다."));

        if (member.isCertifiedByEmail()) {
            return ResponseEntity.badRequest().body("이미 이메일 인증이 완료되었습니다.");
        }

        emailMemberService.sendEmailVerification(email);
        return ResponseEntity.ok("인증 메일이 재전송되었습니다.");
    }
}


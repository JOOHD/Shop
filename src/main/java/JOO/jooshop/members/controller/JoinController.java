package JOO.jooshop.members.controller;

import JOO.jooshop.global.exception.customException.ExistingMemberException;
import JOO.jooshop.global.exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.model.request.JoinMemberRequest;
import JOO.jooshop.members.service.MemberAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class JoinController {

    private final MemberAccountService memberAccountService;
    private final EmailMemberService emailMemberService;

    @GetMapping("/join")
    public String joinPage() {
        return "members/join";
    }

    @PostMapping("/api/join")
    @ResponseBody
    public ResponseEntity<?> join(@RequestBody @Valid JoinMemberRequest request) {
        if (!emailMemberService.isEmailVerified(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("이메일 인증이 필요합니다.");
        }

        try {
            memberAccountService.registerMember(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
        } catch (ExistingMemberException e) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
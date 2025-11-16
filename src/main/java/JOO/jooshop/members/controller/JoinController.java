package JOO.jooshop.members.controller;

import JOO.jooshop.global.exception.customException.ExistingMemberException;
import JOO.jooshop.global.exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;;
import JOO.jooshop.members.model.JoinMemberRequest;
import JOO.jooshop.members.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.function.Consumer;

@Controller
@RequiredArgsConstructor
@Slf4j
public class JoinController {

    private final MemberService memberService;
    private final EmailMemberService emailMemberService;

    @GetMapping("join")
    public String joinPage() {
        return "members/join";
    }

    @PostMapping("/api/join")
    @ResponseBody
    public ResponseEntity<?> join(@RequestBody @Valid JoinMemberRequest request) {
        return handleJoin(request, memberService::registerMember, "회원가입 성공");
    }

    private ResponseEntity<?> handleJoin(
            JoinMemberRequest request,
            Consumer<JoinMemberRequest> joinFunction,
            String successMessage) {

        if (!emailMemberService.isEmailVerified(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("이메일 인증이 필요합니다.");
        }

        try {
            joinFunction.accept(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(successMessage);
        } catch (ExistingMemberException e) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest().body("닉네임을 입력해야 합니다.");
        }
    }
}

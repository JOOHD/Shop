package JOO.jooshop.members.controller;

import JOO.jooshop.global.Exception.customException.ExistingMemberException;
import JOO.jooshop.global.Exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.Exception.customException.UnverifiedEmailException;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.model.JoinMemberRequest;
import JOO.jooshop.members.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.function.Consumer;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class JoinApiController {

    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final EmailMemberService emailMemberService;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody @Valid JoinMemberRequest request) {
        return handleJoin(request, memberService::registerMember, "회원가입 성공");
    }

    @PostMapping("/admin/join")
    public ResponseEntity<?> joinAdmin(@RequestBody @Valid JoinMemberRequest request) {
        return handleJoin(request, memberService::registerAdmin, "관리자 등록 성공");
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


    @PostMapping("/login")
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

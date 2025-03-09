package JOO.jooshop.members.controller;


import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberApiController {

    /*
        1. 회원목록 조회, 비활성화/재활성화/비밀번호 재설정
        2. JWT 를 사용하여 토큰 기반 인증, ADMIN 권한 특정 작업

        getMemberList() : 회원 목록 조회, ADMIN
        deActiveMember() : 회원 비활성화, USER
        reActivateMember() : 회원 재활성화, USER
        rePasswordMember() : 회원 비밀번호 재설정, USER
     */

    private final MemberRepositoryV1 memberRepository;
    private final JWTUtil jwtUtil;

    @GetMapping("/") // 회원 목록 조회
    public ResponseEntity<?> getMemberList(HttpServletRequest request) {
        // 1. 헤더의 Authorization에서 JWT 토큰을 꺼냄.
        String accessToken = extractAccessToken(request);
        // 2. 토큰에서 MemberRole을 추출.
        MemberRole userRole = jwtUtil.getRole(accessToken);
        // 3. 권한 = ADMIN 확인
        if (userRole == MemberRole.ADMIN) {
            // 4. 전체 회원 목록 반환.
            List<Member> allMembers = memberRepository.findAll();
            return ResponseEntity.ok(allMembers);
        } else {
            return unauthorizedResponse("관리자페이지에서, 관리자 권한으로만 회원 전체에 대한 조회가 가능합니다.");
        }
    }

    @PostMapping("/deactivate/{memberId}") // 회원 비활성화
    public ResponseEntity<?> deActivateMember(@PathVariable Long memberId, HttpServletRequest request) {
        return handleMemberActivationDeactivation(memberId, false, "이미 비활성화 된 계정입니다!", "계정이 비활성화 되었습니다.");
    }

    @PostMapping("/reactivate/{memberId}") // 회원 재활성화
    public ResponseEntity<?> reActivateMember(@PathVariable Long memberId, HttpServletRequest request) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        return handleMemberActivationDeactivation(memberId, true, "이미 활성화된 계정입니다!", "계정이 활성화되었습니다.");
    }

    @PostMapping("/repassword/{memberId") // 회원 비밀번호 재설정
    public ResponseEntity<?> rePasswordMember(@PathVariable Long memberId, HttpServletRequest request, @RequestBody ResetPasswordRequest passwordRequest) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        Member existingMember = findExistingMember(memberId);

        if (existingMember == null) {
            return responseStatusAndMessage(HttpStatus.BAD_REQUEST,  "회원가입 되지 않은 유저입니다. 존재하지 않는 유저입니다.");
        }

        if (!isPasswordCorrect(passwordRequest.getPassword(), existingMember)) {
            return responseStatusAndMessage(HttpStatus.BAD_REQUEST, "입력하신 비밀번호는 일치하지 않습니다.");
        }

        resetMemberPassword(passwordRequest, existingMember);
        return responseStatusAndMessage(HttpStatus.OK, "계정의 비밀번호가 재설정되었습니다.");
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization.substring(7);
    }

    private ResponseEntity<?> handleMemberActivationDeactivation(Long memberId, boolean shouldActivate, String alreadyStatusMessage, String successMessage) {
        Optional<Member> optionalMember = memberRepository.findById(memberId);
        if (optionalMember.isPresent()) {
            Member existingMember = optionalMember.get();
            if (existingMember.getIsActive() == shouldActivate) {
                return ResponseEntity.ok().body(alreadyStatusMessage);
            }

            if (shouldActivate) {
                existingMember.activateMember();
            } else {
                existingMember.deActivateMember();
            }
            memberRepository.save(existingMember);
            return ResponseEntity.ok().body(successMessage);
        }
        return ResponseEntity.badRequest().body("존재하지 않는 Id 에 대한 요청입니다.");
    }

    private Member findExistingMember(Long memberId) {
        return memberRepository.findById(memberId).orElse(null);
    }

    private boolean isPasswordCorrect(String password, Member existingMember) {
        return existingMember.getPassword().equals(password);
    }

    private void resetMemberPassword(ResetPasswordRequest passwordRequest, Member existingMember) {
        if (isNewPasswordsMatch(passwordRequest)) {
            existingMember.resetPassword(passwordRequest.getNew_password());
            memberRepository.save(existingMember);
        }
    }

    private boolean isNewPasswordsMatch(ResetPasswordRequest passwordRequest) {
        return passwordRequest.getNew_password().equals(passwordRequest.getNew_password_confirmation());
    }

    private ResponseEntity<?> responseStatusAndMessage(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }

    private ResponseEntity<?> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFoundException(UsernameNotFoundException e) {
        // 회원을 찾지 못한 경우 404 Not Found 응답과 함께 예외 메시지를 반환
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @Data // 내부 DTO 클래스는, 해당 클래스에서만 사용되는 DTO 클래스이다.
    private static class ResetPasswordRequest {
        private String password;
        private String new_password;
        private String new_password_confirmation;
    }
}
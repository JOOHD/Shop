package JOO.jooshop.members.controller;


import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.service.MemberStatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberRepositoryV1 memberRepository;
    private final MemberStatusService memberStatusService;
    private final JWTUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getMemberList(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        MemberRole userRole = jwtUtil.getRole(accessToken);

        if (userRole != MemberRole.ADMIN) {
            return unauthorizedResponse("관리자 권한이 필요합니다.");
        }

        List<Member> members = memberRepository.findAll();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/activate/{memberId}")
    public ResponseEntity<?> activateMember(@PathVariable Long memberId) {
        memberStatusService.activate(memberId);
        return ResponseEntity.ok("계정이 활성화되었습니다.");
    }

    @PostMapping("/deactivate/{memberId}")
    public ResponseEntity<?> deactivateMember(@PathVariable Long memberId) {
        memberStatusService.deactivate(memberId);
        return ResponseEntity.ok("계정이 비활성화되었습니다.");
    }

    @PostMapping("/ban/{memberId}")
    public ResponseEntity<?> banMember(@PathVariable Long memberId) {
        memberStatusService.ban(memberId);
        return ResponseEntity.ok("계정이 정지되었습니다.");
    }

    @PostMapping("/unban/{memberId}")
    public ResponseEntity<?> unbanMember(@PathVariable Long memberId) {
        memberStatusService.unban(memberId);
        return ResponseEntity.ok("계정 정지가 해제되었습니다.");
    }

    @PostMapping("/expire-account/{memberId}")
    public ResponseEntity<?> expireAccount(@PathVariable Long memberId) {
        memberStatusService.expireAccount(memberId);
        return ResponseEntity.ok("계정이 만료되었습니다.");
    }

    @PostMapping("/renew-account/{memberId}")
    public ResponseEntity<?> renewAccount(@PathVariable Long memberId) {
        memberStatusService.renewAccount(memberId);
        return ResponseEntity.ok("계정 만료가 해제되었습니다.");
    }

    @PostMapping("/expire-password/{memberId}")
    public ResponseEntity<?> expirePassword(@PathVariable Long memberId) {
        memberStatusService.expirePassword(memberId);
        return ResponseEntity.ok("비밀번호가 만료 처리되었습니다.");
    }

    @PostMapping("/renew-password/{memberId}")
    public ResponseEntity<?> renewPassword(@PathVariable Long memberId) {
        memberStatusService.renewPassword(memberId);
        return ResponseEntity.ok("비밀번호 만료가 해제되었습니다.");
    }

    @PostMapping("/repassword/{memberId}")
    public ResponseEntity<?> resetPassword(@PathVariable Long memberId,
                                           HttpServletRequest request,
                                           @RequestBody ResetPasswordRequest req) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);
        Member member = findExistingMember(memberId);

        if (!member.getPassword().equals(req.getPassword())) {
            return responseStatusAndMessage(HttpStatus.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다.");
        }

        if (!req.getNew_password().equals(req.getNew_password_confirmation())) {
            return responseStatusAndMessage(HttpStatus.BAD_REQUEST, "새 비밀번호가 서로 일치하지 않습니다.");
        }

        memberStatusService.changePassword(memberId, req.getNew_password());
        return responseStatusAndMessage(HttpStatus.OK, "비밀번호가 변경되었습니다.");
    }

    private Member findExistingMember(Long id) {
        return memberRepository.findById(id).orElse(null);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private ResponseEntity<?> responseStatusAndMessage(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }

    private ResponseEntity<?> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

    @Data
    private static class ResetPasswordRequest {
        private String password;
        private String new_password;
        private String new_password_confirmation;
    }
}

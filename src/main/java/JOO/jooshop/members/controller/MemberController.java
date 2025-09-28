package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.model.MemberResponse;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.service.MemberStatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberRepositoryV1 memberRepository;
    private final MemberStatusService memberStatusService;
    private final JWTUtil jwtUtil;

    /* JWT 쿠키에서 로그인 사용자 정보 조회 */
    @GetMapping("/member-info")
    public ResponseEntity<?> getCurrentMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        return ResponseEntity.ok(MemberResponse.fromMember(member));
    }

    /* 회원 리스트 */
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

    /* 계정 활성화 */
    @PostMapping("/activate/{memberId}")
    public ResponseEntity<?> activateMember(@PathVariable Long memberId) {
        memberStatusService.activate(memberId);
        return ResponseEntity.ok("계정이 활성화되었습니다.");
    }

    /* 계정 비활성화 */
    @PostMapping("/deactivate/{memberId}")
    public ResponseEntity<?> deactivateMember(@PathVariable Long memberId) {
        memberStatusService.deactivate(memberId);
        return ResponseEntity.ok("계정이 비활성화되었습니다.");
    }

    /* 계쩡 정지 */
    @PostMapping("/ban/{memberId}")
    public ResponseEntity<?> banMember(@PathVariable Long memberId) {
        memberStatusService.ban(memberId);
        return ResponseEntity.ok("계정이 정지되었습니다.");
    }

    /* 계정 정지 해게 */
    @PostMapping("/unban/{memberId}")
    public ResponseEntity<?> unbanMember(@PathVariable Long memberId) {
        memberStatusService.unban(memberId);
        return ResponseEntity.ok("계정 정지가 해제되었습니다.");
    }

    /* 계정 만료 */
    @PostMapping("/expire-account/{memberId}")
    public ResponseEntity<?> expireAccount(@PathVariable Long memberId) {
        memberStatusService.expireAccount(memberId);
        return ResponseEntity.ok("계정이 만료되었습니다.");
    }

    /* 계정 만료 해제 */
    @PostMapping("/renew-account/{memberId}")
    public ResponseEntity<?> renewAccount(@PathVariable Long memberId) {
        memberStatusService.renewAccount(memberId);
        return ResponseEntity.ok("계정 만료가 해제되었습니다.");
    }

    /* 비밀번호 만료 */
    @PostMapping("/expire-password/{memberId}")
    public ResponseEntity<?> expirePassword(@PathVariable Long memberId) {
        memberStatusService.expirePassword(memberId);
        return ResponseEntity.ok("비밀번호가 만료 처리되었습니다.");
    }

    /* 비밀번호 만료 해제 */
    @PostMapping("/renew-password/{memberId}")
    public ResponseEntity<?> renewPassword(@PathVariable Long memberId) {
        memberStatusService.renewPassword(memberId);
        return ResponseEntity.ok("비밀번호 만료가 해제되었습니다.");
    }

    /* 비밀번호 리셋 */
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

    /* 가입 회원 찾기
   - 전달받은 memberId로 DB 조회
   - 존재하지 않으면 null 반환 */
    private Member findExistingMember(Long id) {
        return memberRepository.findById(id).orElse(null);
    }

    /* Authorization 헤더에서 Access Token 추출
       - "Bearer " 접두사 제거 후 순수 토큰 반환
       - 헤더 없거나 형식이 맞지 않으면 null 반환 */
    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    /* 공통: 상태 코드 + 메시지 응답 생성
       - 주로 BadRequest, OK 등 일반 응답에 사용 */
    private ResponseEntity<?> responseStatusAndMessage(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }

    /* 공통: 401 Unauthorized 응답 생성
       - 로그인 필요 또는 권한 부족 상황에서 사용 */
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

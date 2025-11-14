package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.MemberResponse;
import JOO.jooshop.members.model.ResetPasswordRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.service.MemberService;
import JOO.jooshop.members.service.MemberStatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final MemberStatusService memberStatusService;

    /* JWT 쿠키에서 로그인 사용자 정보 조회 */
    @GetMapping("/member-info")
    public ResponseEntity<MemberResponse> getCurrentMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        return ResponseEntity.ok(MemberResponse.toEntity(memberService.findMemberById(memberId)));
    }

    /* 회원 리스트 (관리자 권한 필요) */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberResponse>> getMemberList() {
        List<MemberResponse> list = memberService.findAllMembers()
                .stream()
                .map(MemberResponse::toEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /* 계정 활성화 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/activate/{memberId}")
    public ResponseEntity<String> activateMember(@PathVariable Long memberId) {
        memberStatusService.activate(memberId);
        return ResponseEntity.ok("계정이 활성화되었습니다.");
    }

    /* 계정 비활성화 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deactivate/{memberId}")
    public ResponseEntity<String> deactivateMember(@PathVariable Long memberId) {
        memberStatusService.deactivate(memberId);
        return ResponseEntity.ok("계정이 비활성화되었습니다.");
    }

    /* 계쩡 정지 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ban/{memberId}")
    public ResponseEntity<String> banMember(@PathVariable Long memberId) {
        memberStatusService.ban(memberId);
        return ResponseEntity.ok("계정이 정지되었습니다.");
    }

    /* 계정 정지 해게 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unban/{memberId}")
    public ResponseEntity<String> unbanMember(@PathVariable Long memberId) {
        memberStatusService.unban(memberId);
        return ResponseEntity.ok("계정 정지가 해제되었습니다.");
    }

    /* 계정 만료 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/expire-account/{memberId}")
    public ResponseEntity<String> expireAccount(@PathVariable Long memberId) {
        memberStatusService.expireAccount(memberId);
        return ResponseEntity.ok("계정이 만료되었습니다.");
    }

    /* 계정 만료 해제 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/renew-account/{memberId}")
    public ResponseEntity<String> renewAccount(@PathVariable Long memberId) {
        memberStatusService.renewAccount(memberId);
        return ResponseEntity.ok("계정 만료가 해제되었습니다.");
    }

    /* 비밀번호 만료 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/expire-password/{memberId}")
    public ResponseEntity<String> expirePassword(@PathVariable Long memberId) {
        memberStatusService.expirePassword(memberId);
        return ResponseEntity.ok("비밀번호가 만료 처리되었습니다.");
    }

    /* 비밀번호 만료 해제 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/renew-password/{memberId}")
    public ResponseEntity<String> renewPassword(@PathVariable Long memberId) {
        memberStatusService.renewPassword(memberId);
        return ResponseEntity.ok("비밀번호 만료가 해제되었습니다.");
    }

    /* 비밀번호 리셋 */
    @PostMapping("/repassword/{memberId}")
    public ResponseEntity<String> resetPassword(@PathVariable Long memberId,
                                                @RequestBody ResetPasswordRequest req,
                                                @AuthenticationPrincipal Member userDetails) {
        // 본인 계정 검증 (관리자는 검증 제외 가능)
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        memberStatusService.changePassword(memberId, req.getNew_password());
        return responseStatusAndMessage(HttpStatus.OK, "비밀번호가 변경되었습니다.");
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
    private ResponseEntity<String> responseStatusAndMessage(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }

    /* 공통: 401 Unauthorized 응답 생성
       - 로그인 필요 또는 권한 부족 상황에서 사용 */
    private ResponseEntity<String> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }
}

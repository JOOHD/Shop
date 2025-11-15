package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.model.MemberResponse;
import JOO.jooshop.members.model.ResetPasswordRequest;
import JOO.jooshop.members.service.MemberService;
import JOO.jooshop.members.service.MemberStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberApiController {

    private final MemberService memberService;
    private final MemberStatusService memberStatusService;

    /* 로그인 사용자 정보 */
    @GetMapping("/member-info")
    public ResponseEntity<MemberResponse> getCurrentMember(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMemberId();
        return ResponseEntity.ok(
                MemberResponse.toEntity(memberService.findMemberById(memberId))
        );
    }

    /* 본인 비밀번호 변경 */
    @PostMapping("/repassword/{memberId}")
    public ResponseEntity<String> resetPassword(
            @PathVariable Long memberId,
            @RequestBody ResetPasswordRequest req
    ) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        memberStatusService.changePassword(memberId, req.getNew_password());
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
}

package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authorization.MemberAuthorizationUtil;
import JOO.jooshop.members.model.request.ResetPasswordRequest;
import JOO.jooshop.members.model.response.MemberResponse;
import JOO.jooshop.members.service.MemberAccountService;
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
    /*
     * [Controller]
     *
     * 기존
     * - 인증 / 회원정보 / 프로필 / 이메일 인증 로직이 혼재
     * - 하나의 컨트롤러에서 다양한 책임 처리
     * - API 경계가 도메인 기준이 아닌 기능 기준으로 섞여 있음
     *
     * refactoring 26.04
     * - AuthController / MemberApiController 역할 분리
     * - 요청/응답 매핑만 담당 (Thin Controller)
     * - 도메인 기준 API 경계 설정
     */

    private final MemberAccountService memberAccountService;

    @GetMapping("/member-info")
    public ResponseEntity<MemberResponse> getCurrentMember(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMemberId();

        return ResponseEntity.ok(
                MemberResponse.toEntity(memberAccountService.findMemberById(memberId))
        );
    }

    @PostMapping("/repassword/{memberId}")
    public ResponseEntity<String> changePassword(
            @PathVariable Long memberId,
            @RequestBody ResetPasswordRequest request
    ) {
        MemberAuthorizationUtil.verifyUserIdMatch(memberId);

        memberAccountService.changePassword(memberId, request.getNew_password());
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
}
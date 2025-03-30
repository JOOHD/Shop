package JOO.jooshop.global.authorization;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static JOO.jooshop.global.ResponseMessageConstants.*;
@Slf4j
public class MemberAuthorizationUtil {

    /*
        클라이언트 요청 → 컨트롤러 @PathVariable memberId → verifyUserIdMatch(givenId)
                                          ↓
                             getLoginMemberId()  → 현재 로그인한 사용자 ID
                             getLoginMemberRole() → 현재 로그인한 사용자 Role
                                          ↓
                로그인 사용자와 경로에 있는 사용자 ID가 다르면 예외 발생

        1. getCustomUserDetails
        - 현재 인증된 사용자 정보(Authentication) 를 SecurityContextHolder 에서 가져옴.
        - authentication.getPrincipal()로 CustomUserDetails 객체를 얻음.

        JWT token parsing
        - Authorization 헤더에서 토큰을 꺼내고,
        - jwtProvider 가 토큰을 검증 및 파싱해서 사용자 ID와 Role 을 리턴합니다.
     */

    private MemberAuthorizationUtil() {
        throw new AssertionError();
    }

    private static CustomUserDetails getCustomUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) { // 로그인 안 되어 있는 경우
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public static Long getLoginMemberId() {
        try {
            return getCustomUserDetails().getMemberId();
        } catch (ClassCastException e) {
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
    }

    public static MemberRole getLoginMemberRole() {
        try {
            return getCustomUserDetails().getMemberRole();
        } catch (ClassCastException e) {
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
    }

    public static void verifyUserIdMatch(Long givenId) { // order/cart/address/payment/profile, memberId = givenId (요청받은)
        Long loginMemberId = getLoginMemberId();        // 현재 로그인한 사용자 ID
        MemberRole memberRole = getLoginMemberRole();   // 현재 로그인한 사용자 역할

        if (!loginMemberId.equals(givenId) && memberRole != MemberRole.ADMIN) {
            throw new SecurityException(ACCESS_DENIED+" : 요청 사용자와 로그인 사용자 불일치");
        }
    }
}

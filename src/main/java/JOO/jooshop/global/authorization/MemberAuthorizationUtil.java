package JOO.jooshop.global.authorization;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static JOO.jooshop.global.exception.ResponseMessageConstants.*;
@Slf4j
public class MemberAuthorizationUtil {

    /*
        MemberAuthorizationUtil 역할:
        1. 현재 로그인한 사용자의 ID와 Role 정보를 가져옴
        2. 요청 경로의 memberId와 로그인 사용자 ID를 비교
        3. 필요 시 관리자 권한 체크
        4. 권한이 맞지 않으면 SecurityException 발생
     */

    // Utility 클래스이므로 인스턴스화 방지
    private MemberAuthorizationUtil() {
        throw new AssertionError();
    }

    /**
     * 현재 인증된 사용자 정보(CustomUserDetails) 반환
     * - SecurityContextHolder에서 Authentication을 가져옴
     * - 인증 정보 없으면 SecurityException 발생
     */
    private static CustomUserDetails getCustomUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) { // 로그인 안 되어 있는 경우
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * 현재 로그인한 사용자 ID 반환
     * - getCustomUserDetails()에서 ID 추출
     * - ClassCastException 시 인증 실패로 처리
     */
    public static Long getLoginMemberId() {
        try {
            return getCustomUserDetails().getMemberId();
        } catch (ClassCastException e) {
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
    }

    /**
     * 현재 로그인한 사용자 Role 반환
     * - getCustomUserDetails()에서 Role 추출
     * - ClassCastException 시 인증 실패로 처리
     */
    public static MemberRole getLoginMemberRole() {
        try {
            return getCustomUserDetails().getMemberRole();
        } catch (ClassCastException e) {
            throw new SecurityException(ACCESS_DENIED_NO_AUTHENTICATION);
        }
    }

    /**
     * 요청받은 memberId가 현재 로그인 사용자 ID와 일치하는지 확인
     * - 일치하지 않고 ADMIN이 아니면 SecurityException 발생
     * - 개인 프로필 접근, 주문, 장바구니 등에서 사용
     *
     * @param givenId 요청 PathVariable의 memberId
     */
    public static void verifyUserIdMatch(Long givenId) {
        Long loginMemberId = getLoginMemberId();        // 현재 로그인한 사용자 ID
        MemberRole memberRole = getLoginMemberRole();   // 현재 로그인한 사용자 역할

        if (!loginMemberId.equals(givenId) && memberRole != MemberRole.ADMIN) {
            throw new SecurityException(ACCESS_DENIED + " : 요청 사용자와 로그인 사용자 불일치");
        }
    }
}

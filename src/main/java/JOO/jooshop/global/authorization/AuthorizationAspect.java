package JOO.jooshop.global.authorization;

import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.Value;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect // 특정 지점에서 부가적인 처리를 수행할 수 있는 클래스
@Component
public class AuthorizationAspect {

    @Value("${custom.requires-role.enabled:true}") // 프로퍼티 값을 가져옴
    private boolean requiresRoleEnabled;

    @Before("@annotation(requireRole)") // requireRole 실행 전, checkRole 메서드 실행
    public void checkRole(RequiresRole requireRole) {
        if (!requiresRoleEnabled) {
            // requiresRoleEnabled = false, 어노테이션 체크를 스킵
            return;
        }

        // 현재 로그인 사용자 역할 가져오기
        MemberRole userRole = MemberAuthorizationUtil.getLoginMemberRole();

        boolean hasRequiredRole = false;
        for (MemberRole requiredRole : requireRole.value()) {
            if (userRole == requiredRole) {
                hasRequiredRole = true;
                break;
            }
        }

        StringBuilder requiredRolesBuilder = new StringBuilder();

        for (MemberRole requiredRole : requireRole.value()) {
            requiredRolesBuilder.append(requiredRole.toString()).append(", ");
        }

        String requiredRoles = requiredRolesBuilder.toString();
        // 마지막 콤마와 공백 제거
        if (requiredRoles.length() > 0) {
            requiredRoles = requiredRoles.substring(0, requiredRoles.length() - 2);
        }

        if (!hasRequiredRole) {
            throw new SecurityException("사용자 권한 없음\n - Required Roles : " + requiredRoles + ", Request User Role : " + userRole);
        }
    }
}

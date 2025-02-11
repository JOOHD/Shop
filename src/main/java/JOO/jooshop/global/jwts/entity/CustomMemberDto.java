package JOO.jooshop.global.jwts.entity;

import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.Data;

@Data
public class CustomMemberDto {

    /*
        Java에서는 boolean 타입의 필드가 is로 시작하면, Lombok 또는 일반적인 Getter 규칙에 의해 getIsActive()가 아니라 isActive() 형태로 Getter가 자동 생성됩니다.
        하지만 생성자 파라미터에는 is를 붙이지 않는 것이 일반적입니다.

        생성자에서 isActive, isBanned 등의 값을 넘기지 않은 이유
        -단순히 기본값을 사용하고, 나중에 필요한 경우 set() 메서드를 통해 값을 변경할 수 있게 합니다.
     */

    private Long memberId;

    private String email;

    private String username;

    private String password;

    private MemberRole memberRole;

    private boolean isActive;           // 계정 활성화 여부

    private boolean isBanned;           // 계정 정지 여부
    
    private boolean isPasswordExpired;  // 비밀번호 만료 여부
    
    private boolean isAccountExpired;   // 계정 만료 여부

    public CustomMemberDto(Long memberId,
                           String email,
                           String username,
                           String password,
                           MemberRole memberRole,
                           boolean isActive,
                           boolean isBanned,
                           boolean isPasswordExpired, boolean isAccountExpired) {
        this.memberId = memberId;
        this.email = email;
        this.username = username;
        this.password = password;
        this.memberRole = memberRole;
        this.isActive = isActive;
        this.isBanned = isBanned();
        this.isAccountExpired = isAccountExpired();
        this.isPasswordExpired = isPasswordExpired();
    }

    /** 핵심 생성자 */
    public static CustomMemberDto createCustomMember(Member member) {
        return new CustomMemberDto(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getPassword(),
                member.getMemberRole(),
                member.getIsActive(),
                member.getIsBanned(),
                member.getIsAccountExpired(),
                member.getIsPasswordExpired()
        );
    }

    /** 생성자 체이닝: 기본값을 가진 생성자 */
    public CustomMemberDto(Long memberId, String email, String username, String password, MemberRole memberRole) {
        this(memberId,
                email,
                username,
                password,
                memberRole,
                true,
                false,
                false,
                false);
    }

    /** 생성자 체이닝: 최소 정보만 받는 경우 */
    public CustomMemberDto(Long memberId, MemberRole memberRole) {
        this(memberId,
                null,
                null,
                null,
                memberRole,
                true,
                false,
                false,
                false);
    }

    /** 엔티티 -> DTO 변환 */
    public static CustomMemberDto fromEntity(Member member) {
        return new CustomMemberDto(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getPassword(),
                member.getMemberRole(),
                member.getIsActive(),
                false,
                false,
                false
        );
    }

    /** 계정 정지된 사용자 DTO 생성 */
    public static CustomMemberDto bannedMember(Member member) {
        return new CustomMemberDto(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getPassword(),
                member.getMemberRole(),
                false,
                true,
                false,
                false
        );
    }
}

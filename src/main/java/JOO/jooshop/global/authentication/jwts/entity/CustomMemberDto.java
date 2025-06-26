package JOO.jooshop.global.authentication.jwts.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 관련 사용자 정보를 담는 DTO
 * - 인증 처리에서 Member 엔티티 대신 경량화된 정보만 전달
 * - 보안/세션 처리 등에서 주로 사용
 */
@Getter
@Builder
public class CustomMemberDto {

    private Long memberId;
    private String email;
    private String username;
    private String password;
    private MemberRole memberRole;

    private boolean active;             // 계정 활성화 여부
    private boolean banned;             // 정지 여부
    private boolean passwordExpired;    // 비밀번호 만료 여부
    private boolean accountExpired;     // 계정 만료 여부

    // ========================== 정적 생성 메서드 ==========================

    /**
     * Member 엔티티 → CustomMemberDto 변환
     */
    public static CustomMemberDto from(Member member) {
        return CustomMemberDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .password(member.getPassword())
                .memberRole(member.getMemberRole())
                .active(member.isActive())
                .banned(member.isBanned())
                .passwordExpired(member.isPasswordExpired())
                .accountExpired(member.isAccountExpired())
                .build();
    }

    /**
     * 기존 사용했던 메서드 생성자
     */
    public static CustomMemberDto createCustomMember(Member member) {
        return CustomMemberDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .password(member.getPassword())
                .memberRole(member.getMemberRole())
                .active(member.isActive())
                .banned(member.isBanned())
                .passwordExpired(member.isPasswordExpired())
                .accountExpired(member.isAccountExpired())
                .build();
    }

    /**
     * 계정 정지 상태의 사용자 DTO 생성
     */
    public static CustomMemberDto bannedMember(Member member) {
        return CustomMemberDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .password(member.getPassword())
                .memberRole(member.getMemberRole())
                .active(false)
                .banned(true)
                .passwordExpired(false)
                .accountExpired(false)
                .build();
    }

    /**
     * 최소 정보만 포함된 DTO 생성
     */
    public static CustomMemberDto withMinimal(Long memberId, MemberRole memberRole) {
        return CustomMemberDto.builder()
                .memberId(memberId)
                .memberRole(memberRole)
                .active(true)
                .banned(false)
                .passwordExpired(false)
                .accountExpired(false)
                .build();
    }

    /**
     * 테스트 및 기본 세션용 DTO 생성
     */
    public static CustomMemberDto basic(Long memberId, String email, String username,
                                        String password, MemberRole role) {
        return CustomMemberDto.builder()
                .memberId(memberId)
                .email(email)
                .username(username)
                .password(password)
                .memberRole(role)
                .active(true)
                .banned(false)
                .passwordExpired(false)
                .accountExpired(false)
                .build();
    }
}

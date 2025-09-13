package JOO.jooshop.global.authentication.jwts.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security에서 사용하는 UserDetails 구현체
 * - 인증 정보(email, password, role)를 CustomMemberDto에서 가져옴
 * - SecurityContext에 인증 정보를 저장할 때 사용됨
 */
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final CustomMemberDto customMemberDto;

    /**
     * 권한 반환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + customMemberDto.getMemberRole().name()));
    }

    /**
     * 비밀번호 반환
     */
    @Override
    public String getPassword() {
        return customMemberDto.getPassword();
    }

    /**
     * 로그인용 아이디 반환 (email)
     */
    @Override
    public String getUsername() {
        return customMemberDto.getEmail();
    }

    /**
     * 화면용 주문자 이름 반환
     */
    public String getOrdererName() {
        return customMemberDto.getOrdererName();
    }

    /**
     * 사용자 전화번호 반환
     */
    public String getPhoneNumber() {
        return customMemberDto.getPhoneNumber();
    }

    /**
     * 이메일 기준으로 Member 조회 (repository 직접 사용)
     */
    public Member getMember(MemberRepositoryV1 memberRepository) {
        return memberRepository.findByEmail(customMemberDto.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * 회원 ID 반환
     */
    public Long getMemberId() {
        return customMemberDto.getMemberId();
    }

    /**
     * 회원 권한 반환
     */
    public MemberRole getMemberRole() {
        return customMemberDto.getMemberRole();
    }

    /** 계정 만료 여부 */
    @Override
    public boolean isAccountNonExpired() {
        return !customMemberDto.isAccountExpired();
    }

    /** 계정 잠김 여부 */
    @Override
    public boolean isAccountNonLocked() {
        return !customMemberDto.isBanned();
    }

    /** 비밀번호 만료 여부 */
    @Override
    public boolean isCredentialsNonExpired() {
        return !customMemberDto.isPasswordExpired();
    }

    /** 계정 활성화 여부 */
    @Override
    public boolean isEnabled() {
        return customMemberDto.isActive();
    }
}

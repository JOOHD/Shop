package JOO.jooshop.global.jwts.entity;

import JOO.jooshop.members.entity.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    /*
        UserDetails : Spring Security 가 사용자 정보를 관리하기 위해 사용하는 인터페이스
        UserDetailsService 가 UserDetails 객체를 반환하고, SecurityContext 에서 인증 정보를 저장할 때 사용됨.
        기존 UserDetails 에서 인증 정보(email, password, role)를 CustomMemberDto 에서 가져옴.
    */

    private final CustomMemberDto customMemberDto;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        // SimpleGrantedAuthority 를 사용, '' 말고 "" 사용해야됨(문자, 문자열 리터럴 혼동)
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + customMemberDto.getMemberRole().name()));
        return authorities;

    }

    /*
        refactoring (isActive 값이 true, false 통일되는 문제, 모든 상태를 하나의 값으로 통제하는 것은 위험)
        각 상태(만료, 잠김, 비밀번호 만료, 활성화)가 각각 다를 수 있기 때문입니다.
     */
    @Override
    public String getPassword() {
        return customMemberDto.getPassword(); // 사용자의 비밀번호 반환
    }

    @Override
    public String getUsername() {
        return customMemberDto.getEmail(); // 사용자의 이메일 반환
    }

    public Long getMemberId() { return customMemberDto.getMemberId(); }

    public MemberRole getMemberRole(){
        return customMemberDto.getMemberRole(); // 사용자의 권한 반환
    }

    /**  계정이 만료되지 않았는가? */
    @Override
    public boolean isAccountNonExpired() {
        return !customMemberDto.isAccountExpired();
    }

    /**  계정이 잠기지 않았는가? */
    @Override
    public boolean isAccountNonLocked() {
        return !customMemberDto.isBanned();
    }

    /**  비밀번호가 만료되지 않았는가? */
    @Override
    public boolean isCredentialsNonExpired() {
        return !customMemberDto.isPasswordExpired();
    }

    /**  계정이 활성화되었는가? */
    @Override
    public boolean isEnabled() {
        return customMemberDto.isActive();
    }
}

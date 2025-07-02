package JOO.jooshop.global.authentication.jwts.service;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.entity.CustomMemberDto;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberService memberService;

    // 사용자 로그인 -> CustomUserDetailsService(loadUserByUsername 반환) -> CustomUserDetails -> JWTFilter 
    // 인증된 사용자 -> Member -> CustomMemberDto -> CustomUserDetailsSerivce -> JWTFilter, Authentication 으로 보내짐.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberService.findMemberByEmail(email);
        if (!member.isCertifiedByEmail()) {
            throw new AuthenticationServiceException("Email is not certified yet.");
        }

        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);

        return new CustomUserDetails(customMemberDto);
    }
}

package JOO.jooshop.global.jwts.service;

import JOO.jooshop.global.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.jwts.entity.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {

        Member member = memberService.validateDuplicatedEmail(email);

        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);

        return new CustomUserDetails(customMemberDto);
    }
}

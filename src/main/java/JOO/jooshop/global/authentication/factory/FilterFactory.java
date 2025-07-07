package JOO.jooshop.global.authentication.factory;

import JOO.jooshop.global.authentication.jwts.filters.JWTFilterV3;
import JOO.jooshop.global.authentication.jwts.filters.LoginFilter;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterFactory {

    private final ObjectMapper objectMapper;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public LoginFilter createLoginFilter(AuthenticationManager authenticationManager, MemberService memberService) {
        LoginFilter loginFilter = new LoginFilter(
                authenticationManager,
                objectMapper,
                memberService,
                jwtUtil,
                refreshRepository
        );
        loginFilter.setFilterProcessesUrl("/api/login");
        return loginFilter;
    }

    public JWTFilterV3 createJWTFilter(MemberService memberService) {
        return new JWTFilterV3(jwtUtil, redisTemplate, memberService);
    }
}

package JOO.jooshop.global.jwts.controller;


import JOO.jooshop.global.jwts.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController {

    /*
        ※ 사용자 인증 전체 흐름
        1. 사용자 로그인 요청 : (id(email), password)
        2. CustomUserDetailsService : 사용자 정보 조회 (loadUserByUsername method)
        3. CustomUserDetails : 사용자 세부 정보 (UserDetails interface implements)
        4. JWTFilter : JWT 토큰 인증 (parse(추출), validation(검증), authentication(인증))
        5. SecurityContextHolder : 인증 정보 보관
     */

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final MemberRepositoryV1 memberRepositoryV1;
    /**
     * long accessTokenExpirationPeriod = 60L * 30; 30 분
     * long refreshTokenExpirationPeriod = 3600L * 24 * 7; 7일
     */
    private Long accessTokenExpirationPeriod = 60L * 30; // 30 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일
}

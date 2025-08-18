package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.service.CookieService;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.jwts.utils.TokenResolver;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.service.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JWTFilterV3 extends OncePerRequestFilter {

    /*
        JWTFilterV3 역할 및 인증 흐름 설명

        1. 클라이언트 요청: /api/** (JWT 인증이 필요한 API)
        2. AccessToken & RefreshToken 확인
            - Authorization 헤더 또는 accessToken 쿠키에서 가져오기
            - refreshAuthorization 쿠키에서 가져오기
        3. RefreshToken 검증
            - 만료 여부 확인
            - 유효하지 않으면 인증 없이 다음 필터로 전달
        4. AccessToken 검증
            - 블랙리스트 체크(Redis)
            - 만료 여부 확인
            - 헤더 또는 쿠키에서 가져온 토큰 사용
        5. 인증 정보 생성
            - Member 조회 → CustomMemberDto → CustomUserDetails
            - SecurityContextHolder에 UsernamePasswordAuthenticationToken 저장
        6. 정적 리소스, 로그인 페이지, /api 외 요청은 필터 적용 안함
     */

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 URI 및 토큰 정보 로그
        Optional<String> authorizationOpt = TokenResolver.resolveTokenFromHeader(request); // 헤더 토큰
        Optional<String> accessTokenCookieOpt = TokenResolver.resolveTokenFromCookie(request, "accessToken"); // 쿠키 토큰
        Optional<String> refreshAuthorizationOpt = TokenResolver.resolveTokenFromCookie(request, "refreshAuthorization"); // refreshToken 쿠키

        log.info("[JWT Filter] 요청 URI: {}", request.getRequestURI());
        log.info("[JWT Filter] Authorization 헤더: {}", authorizationOpt);
        log.info("[JWT Filter] accessToken 쿠키: {}", accessTokenCookieOpt);
        log.info("[JWT Filter] RefreshAuthorization 쿠키: {}", refreshAuthorizationOpt);

        // 2. RefreshToken 없으면 인증 처리 없이 통과
        if (refreshAuthorizationOpt.isEmpty()) {
            log.warn("[JWTFilter] RefreshToken 없음");
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = refreshAuthorizationOpt.get();

        // 3. RefreshToken 유효성 검사
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("[JWTFilter] RefreshToken 유효하지 않음");
            filterChain.doFilter(request, response);
            return;
        }

        // 4. AccessToken 가져오기 (헤더 우선, 없으면 쿠키)
        String accessToken = authorizationOpt.or(() -> accessTokenCookieOpt).orElse(null);

        if (accessToken != null) {

            log.info("[JWTFilter] AccessToken 존재, 검증 진행: {}", accessToken);

            // 5. 블랙리스트 확인
            String blacklistValue = redisTemplate.opsForValue().get("blacklist:" + accessToken);
            if (blacklistValue != null) {
                log.warn("[JWTFilter] 블랙리스트 토큰, 인증 거부");
                filterChain.doFilter(request, response);
                return;
            }

            // 6. 만료 여부 확인
            if (jwtUtil.isExpired(accessToken)) {
                log.warn("[JWTFilter] AccessToken 만료됨");
                filterChain.doFilter(request, response);
                return;
            }

            // 7. 토큰 기반 인증 설정
            try {
                Authentication authToken = getAuthentication(accessToken);
                if (authToken != null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[JWTFilter] SecurityContextHolder에 인증 정보 설정 완료: {}", authToken.getPrincipal());
                }
            } catch (Exception e) {
                log.warn("[JWTFilter] 인증 정보 설정 실패: {}", e.getMessage());
            }

        } else {
            log.warn("[JWTFilter] AccessToken 없음 (헤더/쿠키 모두 없음)");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 정적 리소스나 로그인 페이지는 스킵
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") || uri.equals("/login")) {
            return true;
        }

        // /api/** 요청만 필터 적용
        return !uri.startsWith("/api");
    }

    /**
     * AccessToken으로 인증 정보 생성
     */
    private Authentication getAuthentication(String token) {
        log.info("[getAuthentication] 토큰으로 인증 시도");

        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("유효하지 않은 토큰입니다.");
        }

        String memberId = jwtUtil.getMemberId(token);
        log.info("[getAuthentication] memberId from token: {}", memberId);

        Member member;
        try {
            member = memberService.findMemberById(Long.valueOf(memberId));
        } catch (Exception e) {
            log.warn("[getAuthentication] 사용자 조회 실패 (memberId: {})", memberId);
            return null;
        }

        log.info("[getAuthentication] 사용자 조회 성공: {}", member.getEmail());

        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);
        CustomUserDetails customUserDetails = new CustomUserDetails(customMemberDto);
        log.info("[getAuthentication] CustomUserDetails 생성 완료: {}", customUserDetails.getUsername());

        return new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );
    }
}

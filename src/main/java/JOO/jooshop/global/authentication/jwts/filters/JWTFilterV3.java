package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.service.CookieService;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.jwts.utils.TokenResolver;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.service.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JWTFilterV3 extends OncePerRequestFilter {

    /*
        ※ 사용자 인증 전체 흐름
        1. 사용자 로그인 요청 : (id(email), password)
        2. CustomUserDetailsService : 사용자 정보 조회 (loadUserByUsername method)
        3. CustomUserDetails : 사용자 세부 정보 (UserDetails interface implements)
        4. JWTFilter : JWT 토큰 인증 (parse(추출), validation(검증), authentication(인증))
                        사용자 정보 SecurityContextHolder 에 저장
        5. SecurityContextHolder : 인증 정보 보관

        JWTFilterV0,1,2,3 설명
        V0 : 기본 구조
            - doFilterInternal 메서드에서 Authorization 헤더를 검증하여,
                JWT 토큰을 추출하고, 유효성 검사를 통해 인증 정보를 설정하는 단순한 흐름

        V1 : 쿠키와 헤더 검증 추가
            - Authorization 헤더와 refreshAuthorization 쿠키를 함께 처리하여 토큰 검증을 확장

        V2 : 토큰 만료 검증 및 사용자 인증 흐름 추가
            - refreshToken이 만료되지 않았을 경우 사용자 인증을 처리하고, 만약 만료되었다면 로그아웃 처리를 추가

        V3 : 최종 인증 처리 및 Redis 기반 보안 강화
            - AccessToken 과 RefreshToken 모두를 검증하여 사용자 인증 흐름 완성
            - AccessToken 이 유효할 경우 SecurityContextHolder 에 인증 정보 저장
            - Redis 에 저장된 블랙리스트를 체크하여 로그아웃된 토큰 차단
            - RefreshToken 으 Redis 에 저장된 값과 일치하는지 확인하여 검증
            - Stateless 구조를 유지하면서 Redis 를 통한 인증 상태 제어로 보안성을 높임
     */

    private final JWTUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 헤더에서 Access Token 시도
        Optional<String> authorizationOpt = TokenResolver.resolveTokenFromHeader(request);

        // 쿠키에서 Access Token 시도
        Optional<String> accessTokenCookieOpt = TokenResolver.resolveTokenFromCookie(request, "access_token");

        // Refresh Token 쿠키에서 가져오기
        Optional<String> refreshAuthorizationOpt = TokenResolver.resolveTokenFromCookie(request, "refreshToken");

        log.info("[JWT Filter] 요청 URI: {}", request.getRequestURI());
        log.info("[JWT Filter] Authorization 헤더: {}", authorizationOpt);
        log.info("[JWT Filter] access_token 쿠키: {}", accessTokenCookieOpt);
        log.info("[JWT Filter] RefreshAuthorization 쿠키: {}", refreshAuthorizationOpt);

        if (refreshAuthorizationOpt.isEmpty()) {
            log.warn("RefreshToken이 존재하지 않음");
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorizationOpt.or(() -> accessTokenCookieOpt).orElse(null);

        if (accessToken == null) {
            log.warn("[JWTFilter] AccessToken 없음");
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = refreshAuthorizationOpt.get();

        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("[JWT Filter] RefreshToken 유효하지 않음");
            filterChain.doFilter(request, response);
            return;
        }

        if (authorizationOpt.isPresent()) {
            log.info("[JWTFilter AccessToken 존재 및 Bearer 확인됨: {}", accessToken);

            // 블랙리스트 체크
            String blacklistValue = redisTemplate.opsForValue().get("blacklist:" + accessToken);
            if (blacklistValue != null) {
                log.warn("[JWTFilter] 블랙리스트에 포함된 토큰입니다. 인증 거부");
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtUtil.isExpired(accessToken)) {
                log.warn("[JWTFilter] AccessToken 만료됨");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                Authentication authToken = getAuthentication(accessToken);
                if (authToken != null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[JWTFilter] SecurityContext 에 인증 정보 설정 완료: {}", authToken.getPrincipal());
                }
            } catch (Exception e){
                    log.warn("[JWTFilter] 인증 정보 설정 실패 : {}", e.getMessage());
            }

        } else {
            log.warn("[JWTFilter] Authorization 헤더 없음 또는 형식 이상함");
        }

        filterChain.doFilter(request, response);
    }

    private Authentication getAuthentication(String token) {
        log.info("[getAuthentication] 토큰으로 인증 시도 중");

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






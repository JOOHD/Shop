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
@Component
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

        V3 : 최종 인증 처리 및 코드 정리
            - 인증 흐름을 명확하게 정의하고, accessToken과 refreshToken을 모두 검증한 후,
                최종적으로 인증된 사용자를 SecurityContextHolder에 저장
     */

    private final JWTUtil jwtUtil;
    private final MemberRepositoryV1 memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization Header에서 accessToken 가져오기
        Optional<String> authorizationOpt = TokenResolver.resolveTokenFromHeader(request);

        // Refresh Token 쿠키에서 가져오기
        Optional<String> refreshAuthorizationOpt = TokenResolver.resolveTokenFromCookie(request, "refreshToken");

        log.info("[JWT Filter] 요청 URI: {}", request.getRequestURI());
        log.info("[JWT Filter] Authorization 헤더: {}", authorizationOpt);
        log.info("[JWT Filter] RefreshAuthorization 쿠키: {}", refreshAuthorizationOpt);

        // refreshToken이 없거나 형식이 잘못된 경우, 필터 처리
        if (refreshAuthorizationOpt.isEmpty()) {
            log.warn("[JWT Filter] RefreshToken 없음 또는 형식 이상");
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = refreshAuthorizationOpt.get();
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("[JWT Filter] RefreshToken 유효하지 않음");
            filterChain.doFilter(request, response);
            return;
        }

        // accessToken이 존재하면 검증
        if (authorizationOpt.isPresent()) {
            String accessToken = authorizationOpt.get();
            log.info("[JWT Filter] AccessToken 존재 및 Bearer 확인됨: {}", accessToken);

            // AccessToken이 만료되었는지 체크
            if (jwtUtil.isExpired(accessToken)) {
                log.warn("[JWT Filter] AccessToken 만료됨");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // 토큰을 기반으로 Authentication 객체 생성
                Authentication authToken = getAuthentication(accessToken);

                if (authToken != null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[JWT Filter] SecurityContext 에 인증 정보 설정 완료: {}", authToken.getPrincipal());
                } else {
                    log.warn("[JWT Filter] 인증 정보가 null 입니다. SecurityContext 설정 생략");
                }

            } catch (Exception e) {
                log.warn("[JWT Filter] 인증 정보 설정 실패: {}", e.getMessage());
            }
        } else {
            log.warn("[JWT Filter] Authorization 헤더 없음 또는 형식 이상함");
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

        Optional<Member> optionalMember = memberRepository.findById(Long.valueOf(memberId));
        if (optionalMember.isEmpty()) {
            log.warn("[getAuthentication] 사용자 없음 (memberId: {})", memberId);
            return null;
        }

        Member member = optionalMember.get();
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






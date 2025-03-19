package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.service.CookieService;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

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

        V3 : 최종 인증 처리 및 코드 정리
            - 인증 흐름을 명확하게 정의하고, accessToken과 refreshToken을 모두 검증한 후,
                최종적으로 인증된 사용자를 SecurityContextHolder에 저장
     */

    private final JWTUtil jwtUtil;
    private final CookieService cookieService;
    private final MemberRepositoryV1 memberRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // request 에서 Authentication(accessToken) 헤더 찾음
        String authorization = request.getHeader("Authorization");
        // 쿠키에서 "refreshAuthorization(refreshToken)" 값을 가져 옴
        String refreshAuthorization = cookieService.getRefreshAuthorization(request);

        if (refreshAuthorization == null || !refreshAuthorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String refreshToken = refreshAuthorization.substring(7); // "Bearer " 이후 부분만 처리
        if (!jwtUtil.validateToken(refreshToken)) {
            filterChain.doFilter(request, response);  // refresh token이 유효하지 않으면 넘김
            return;
        }

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.split(" ")[1];

            if (jwtUtil.isExpired(accessToken)) {
                filterChain.doFilter(request, response);  // 만료된 access token 처리
                return;
            }

            Authentication authToken = getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private Authentication getAuthentication(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("유효하지 않은 토큰입니다.");
        }

        String memberId = jwtUtil.getMemberId(token);
        Member member = memberRepository.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));

        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);
        // customUserDetails 로 통일
        CustomUserDetails customUserDetails = new CustomUserDetails(customMemberDto);

        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }
}






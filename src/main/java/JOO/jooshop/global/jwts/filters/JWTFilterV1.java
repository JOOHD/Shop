package JOO.jooshop.global.jwts.filters;

import JOO.jooshop.global.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.jwts.entity.CustomUserDetails;
import JOO.jooshop.members.entity.enums.MemberRole;
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

@Slf4j
@RequiredArgsConstructor
public class JWTFilterV1 extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // request 에서 Authroization 헤더 찾음
        String authorization = request.getHeader("Authorization");

        // 현재 시작을 "년-월-일"으로
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Authorization 헤더가 비어있거나 "Bearer " 로 시작하지 않은 경우
        if (isAuthorizationInvalidOrNotBearer(request, response, filterChain, authorization, currentDate)) return;

        // Authorization 에서 Bearer 접두사 제거 (split = 공백을 기준으로 나눈다.)
        String accessToken = authorization.split(" ")[1];

        // Authorization 에 있는 AccessToken 유효기간이 만료한 경우
        if (isAccessTokenExpired(request, response, filterChain, accessToken, currentDate)) return;

        // access 에 있는 username, role 을 통해 Authorization 사용자 정보를 검증 후, 저장
        Authentication authToken = getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private boolean isAccessTokenExpired(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String accessToken, String currentDate) throws IOException, ServletException {
        if(jwtUtil.isExpired(accessToken)) {
            String memberId = jwtUtil.getMemberId(accessToken);

            log.info("access token 이 만료되었습니다.");
            if (memberId != null) {
                log.info("memberId : " + memberId + " now : " + currentDate);
            }
            filterChain.doFilter(request, response);
            // 메서드 종료
            return true;
        }
        return false;
    }

    // Authorization 헤더가 비어있거나 "Bearer " 로 시작하지 않은 경우
    private static boolean isAuthorizationInvalidOrNotBearer(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String authorization, String currentDate) throws IOException, ServletException {
        if(authorization == null || authorization.startsWith("Bearer ")){
            log.info("로그인 하지 않은 상태이거나, Authorization 을 Request Header 에 담아주지 않았습니다. ");
            log.info(" now : " + currentDate);
            // 토큰이 유효하지 않으므로 request, response 를 다음 필터로 넘겨줌
            filterChain.doFilter(request, response);
            // 메서드 종료
            return true;
        }
        return false;
    }

    private Authentication getAuthentication(String token, Member member) {
        if (!jwtUtil.validateToken(token)) {
            // 토큰이 유효하지 않을 경우 예외 처리
            throw new BadCredentialsException("유효하지 않은 토큰입니다.");
        }

        String memberId = jwtUtil.getMemberId(token);
        MemberRole role = jwtUtil.getRole(token);

        // CustomMemberDto 객체 생성
        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);

        // CustomUserDetails 객체 생성
        CustomUserDetails customOAuth2User = new CustomUserDetails(customMemberDto);
        
        // 인증 객체 반환
        return new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
    }
}

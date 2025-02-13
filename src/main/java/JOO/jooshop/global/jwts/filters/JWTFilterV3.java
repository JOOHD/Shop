package JOO.jooshop.global.jwts.filters;

import JOO.jooshop.global.jwts.entity.CustomMemberDto;
import JOO.jooshop.global.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.jwts.service.CookieService;
import JOO.jooshop.global.jwts.utils.JWTUtil;
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
        5. SecurityContextHolder : 인증 정보 보관
     */

    private final JWTUtil jwtUtil;
    private final CookieService cookieService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // request 에서 Authentication(accessToken) 헤더 찾음
        String authorization = request.getHeader("Authorization");
        // 쿠키에서 "refreshAuthorization(refreshToken)" 값을 가져 옴
        String refreshAuthorization = cookieService.getRefreshAuthrization(request);
        if (refreshAuthorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String refreshToken = Objects.requireNonNull(refreshAuthorization).substring(7);
        log.info("Id : " + jwtUtil.getMemberId(refreshToken) + " 유저가 로그인 했습니다.");

        // 현재 시각을 "년-월-일"으로
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // refreshAuthorization 쿠키 검증
        if (!refreshAuthorization.startsWith("Bearer+")) {

            log.info("로그인 하지 않은 상태이거나, refreshAuthorization 을 Request Header 에 담아주지 않았습니다.");
            log.info(" now : " + currentDate);
            // 토큰이 유효하지 않으므로 request 와response 를 다음 필터로 넘겨줌
            filterChain.doFilter(request, response);
            // 메서드 종료
            return;
        }

        // accessToken 유효기간이 만료한 경우 메서드 종료, API 사용 시, Request Header 에 Authorization 을 담아주는 상황
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.split(" ")[1];

            if (jwtUtil.isExpired(accessToken)) {
                String memberId = jwtUtil.getMemberId(accessToken);

                log.info("access token 이 만료되었습니다.");
                if (memberId != null) {
                    log.info("memberId : " + memberId + " now : " + currentDate);
                }
                filterChain.doFilter(request, response);
                // 메서드 종료
                return;
            }
            // access 에 있는 username, role 을 통해 Authentication 사용자 정보를 검증 후, 저장
            Authentication authToken = getAuthentication(refreshToken);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        }
    }

    private Authentication getAuthentication (String token, Member member){
        if (!jwtUtil.validateToken(token)) {
            // 토큰이 유효하지 않을 경우 예외 처리
            throw new BadCredentialsException("유효하지 않은 토큰입니다.");
        }

        String memberId = jwtUtil.getMemberId(token);
        MemberRole role = jwtUtil.getRole(token);

        CustomMemberDto customMemberDto = CustomMemberDto.createCustomMember(member);

        CustomUserDetails customOAuth2User = new CustomUserDetails(customMemberDto);
        return new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
    }

}






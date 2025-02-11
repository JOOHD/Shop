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
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JWTFilterV2 extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CookieService cookieService;
    
    /*
       1. 쿠키에 refreshAuthorization 존재 O, 로컬스토리지 Authorization 이 존재 O
          - API Request Header 에 Authorization 을 담은 상황
       2. 쿠키에 refreshAuthorization 존재 X, 로컬스토리지 Authorization 이 존재 X
          - API Request Header 에 Authorization 을 담지 않은 상황
       3. 쿠키에 refreshAuthorization 존재 X, 로컬스토리지에 Authorization 이 존재 X
          - 로그아웃 상황
          [결론] JWTFilterV1 필터 단에서는,
          쿠키에 refreshAuthorization 존재한다 = 로그인, 쿠키에 refreshAuthorization 존재하지 않는다 = 로그아웃
         @param request
         @param response
         @param filterChain
         @throws ServletException
         @throws IOException
    */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)throws ServletException, IOException {
        // request 헤더 에서 Authorization 을 가져옵니다.
        String authorization = request.getHeader("Authorization");
        // 쿠키에서 "refreshAuthorization" 값을 가져 옴
        String refreshAuthorization = cookieService.getRefreshAuthorization(request);
        System.out.println("authorization = " + authorization);
        System.out.println("refreshAuthorization = " + refreshAuthorization);

        // TODO 1. Authorization 헤더가 존재하지 않을때 검증
        String accessToken = substringAccessFromAuthorization(request, response, filterChain, authorization);
        // TODO 2. RefreshAuthorization Cookie 검증
        if (validateRefreshAuthorizationInCookie(request, response, filterChain, refreshAuthorization)) return;

        String refreshToken = Objects.requireNonNull(refreshAuthorization).substring(7);

        log.info("accessToken = " + accessToken);
        log.info("refreshToken = " + refreshToken);

        // TODO 3. refreshToken 이 만료시 - 재로그인 하도록 로그아웃 시켜야함.
        if(jwtUtil.isExpired(refreshToken)) {
            String memberId = jwtUtil.getMemberId(refreshToken);

            log.info("Refresh Token 이 만료되었습니다.");
            if (memberId != null) {
                log.info("Refresh Token 이 만료된 회원 Id : " + memberId);
                logCurrentDate();
            }
            return;
        }

        // TODO 4. refreshToken 이 만료되지 않았을 때 - 로그인 상황
        // Authorization 이 존재할때, 엑세스토큰에 대한 유효성 검증을 시작합니다.
        validateAuthorizationExpired(request, response, filterChain, authorization);

        log.info("ID :" + jwtUtil.getMemberId(refreshToken) , " 인 유저가 로그인 중입니다.");
        // TODO 5. 로그인 되어 있는 상황에 대한 검증이 끝난 상황, 다음 필터로 request와 response를 넘겨줍니다.
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }

        // TODO 최초 로그인 시, 클라이언트에서 Authentication 을 Request Header 에 담고, Access Token 을 검증합니다.
        // access 에 있는 memberId, role 을 통해 Authentication 사용자 정보를 Authentication 으로 넘겨줍니다.
        Authentication authToken = getAuthentication(refreshToken);
        // 정상적으로 인증된 멤버라는 것을, SecurityContextHolder 에 넘겨준다.
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private static boolean validateRefreshAuthorizationInCookie(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String refreshAuthorization) throws IOException, ServletException {
        if (refreshAuthorization == null) {
            log.info("refreshAuthorization 이 존재하지 않습니다.");
            return true;
        }
        boolean isRefreshValid = validateRefreshInCookie(request, response, refreshAuthorization);

        if (!isRefreshValid) {
            return true;
        }
        return false;
    }

    private static String substringAccessFromAuthorization(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String authorization) throws IOException, ServletException {
        log.info("Authorization Header: " + authorization);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            // 메서드 종료
            return null;
        }
        return authorization.substring(7);
    }

    private static boolean validateRefreshInCookie(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String requestAuthorization) throws IOException, ServletException {
        if(!requestAuthorization.startsWith("Bearer+")) {
            logCurrentDate();
            log.info("로그아웃 상태입니다.");
            // 토큰이 유효하지 않으므로 request 와 response 를 다음 필터로 넘겨줌
//            filterChain.doFilter(request, response);
            // 메서드 종료
            return true;
        }
        return false;
    }

    private static void nextFilterChainDo(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateAuthorizationExpired(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String authorization) throws IOException {
        // "Bearer " 접두사가 있는 경우만 처리
        if (authorization == null || authorization.equals("undefined") || !authorization.startsWith("Bearer ")) {
            log.info("Authorization 헤더가 존재하지 않거나 'undefined'입니다.");
            logCurrentDate();
            // Abort the request
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization 헤더가 존재하지 않습니다.");
            return;
        } else {
            if (Objects.requireNonNull(authorization).startsWith("Bearer ")) {
                String accessToken = authorization.split(" ")[1];
                if (accessToken.equals("undefined")) {
                    throw new BadCredentialsException("Authorization 헤더에 유효한 access token이 존재하지 않습니다.");
                }
                // 유효기간이 만료한 경우
//                    String memberId = jwtUtil.getMemberId(accessToken);
                log.info("access token 이 만료되었습니다.");
//                    if (memberId != null) {
//                        log.info("memberId : " + memberId);
//                        logCurrentDate();
//                    }
                try {
                    filterChain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void logCurrentDate() {
        // 현재 시각 로깅
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("현재시각 : " + currentDate);
    }

    private Authentication getAuthentication(String token, Member member) {
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
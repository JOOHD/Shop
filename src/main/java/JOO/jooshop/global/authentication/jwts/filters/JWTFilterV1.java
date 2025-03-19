package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.authentication.jwts.entity.CustomMemberDto;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JWTFilterV1 extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CookieService cookieService;
    private final MemberRepositoryV1 memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        // 쿠키 검증 로직 추가
        String refreshAuthorization = cookieService.getRefreshAuthorization(request);

        if (refreshAuthorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!refreshAuthorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // refreshAuthorization 없으면 그냥 넘김
            return;
        }

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.split(" ")[1];

            if (jwtUtil.isExpired(accessToken)) {
                filterChain.doFilter(request, response); // accessToken 이 만료되면 넘어감
                return;
            }
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    jwtUtil.getMemberId(accessToken), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
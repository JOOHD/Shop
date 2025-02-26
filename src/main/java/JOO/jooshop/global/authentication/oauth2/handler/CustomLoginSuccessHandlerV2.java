package JOO.jooshop.global.authentication.oauth2.handler;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.entity.CustomOAuth2User;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Component
public class CustomLoginSuccessHandlerV2 extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    private final MemberRepositoryV1 memberRepository;

    private final RefreshRepository refreshRepository;

    private Long refershTokenExpirationPeriod = 1209600L;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 커스텀 클래스이기 때문에 캐스팅
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getName();
        String role = extractOAuthRole(authentication);
        log.info("=============소셜 로그인 성공, 유저 데이터 시작 ==============");
        log.info("email = " + email);
        log.info("role = " + role);
        log.info("=============소셜 로그인 성공, 유저 데이터 시작 ==============");
        log.info("============= memberId 를 가져오기 위해, DB 조회 시작 ==============");
        Member requestMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일이 존재하지 않습니다."));
        log.info("============= memberId 를 가져오기 위해, DB 조회 끝 ==============");
        log.info("requestMember = " + requestMember);
        // 엑세스 토큰을 생성
        String newAccess = jwtUtil.createAccessToken("access", String.valueOf(requestMember.getId()), role);
        // 리프레쉬 토큰을 생성
        String newRefresh = jwtUtil.createRefreshToken("refresh", String.valueOf(requestMember.getId()), role);
        log.info("newAccess : " + newAccess);
        log.info("newRefresh : " + newRefresh);

        // [Refresh 토큰 - DB에서 관리한다.] 리프래쉬 토큰 관리권한이 서버에 있다.
        saveOrUpdateRefreshEntity(requestMember, newRefresh);

        // [response.data] 에 Json 형태로 accessToken 과 refreshToken 을 넣어주는 방식
        setTokenResponseV2(response, newAccess, newRefresh);

        response.sendRedirect(frontendUrl);
    }

    private static String extractOAuthRole(Authentication authentication) {
        // 사용자 권한 가져오기 default code
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        return role;
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
        // cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

    private void saveOrUpdateRefreshEntity(Member member, String newRefreshToken) {
        // 멤버의 PK 식별자로, refresh 토큰을 가져온다.
        Optional<Refresh> existedRefresh = refreshRepository.findByMemberId(member.getId());
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);
        if (existedRefresh.isPresent()) {
            // 로그인 이메일과 같은 이메일을 가지고 있는 Refresh 앤티티에 대해서, refresh 값을 새롭게 업데이트해줌

        }

    }
}

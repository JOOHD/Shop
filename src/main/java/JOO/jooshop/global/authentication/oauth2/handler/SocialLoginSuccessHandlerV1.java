package JOO.jooshop.global.authentication.oauth2.handler;

import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.entity.CustomOAuth2User;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import JOO.jooshop.members.model.RefreshDto;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class SocialLoginSuccessHandlerV1 extends SimpleUrlAuthenticationSuccessHandler {

    /*
        클래스 역할
        - OAuth2(social login) 성공 후, 실행
        - 사용자 정보 확인, access/refresh 발급
        - refresh DB(보안), Cookie(클라이언트-서버 간 자동 인증) 쿠키 기반 인증 방식
        - 로그인 성공 후, frontend 로 redirect
     */
    private final JWTUtil jwtUtil;
    private final MemberRepositoryV1 memberRepository;
    private final RefreshRepository refreshRepository;

    private Long accessTokenExpirationPeriod = 60L * 12; // 12 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getName();
        String role = extractOAuthRole(authentication);
        String socialId = oAuth2User.getSocialId();

        log.info("소셜 로그인 유저 = " + email);
        // ============= RefreshToken 생성 시, memberId 가 필요 ==============
        Member requestMember = memberRepository.findBySocialId(socialId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 socialId 을 가진 멤버가 존재하지 않습니다."));
        // 토큰을 생성하는 부분
        String refreshToken = jwtUtil.createRefreshToken("refresh", String.valueOf(requestMember.getId()), role);

        // 리프레쉬 토큰 - DB 에 저장
        saveOrUpdateRefreshEntity(requestMember, refreshToken);

        // 리프레쉬 토큰을 쿠키에 저장
        // Header response = Set-Cookie: refreshAuthorization=Bearer%20xxxxx.yyy.zzz; Max-Age=1209600; Path=/; HttpOnly; Secure; SameSite=None
        CookieUtil.createCookieWithSameSite(response, "refreshAuthorization", "Bearer " + refreshToken, 1209600);
        response.setStatus(HttpStatus.OK.value());

        // frontendUrl 을 사용하여 리다이렉션 URL 을 구성
        response.sendRedirect(frontendUrl + "?redirectedFromSocialLogin=true");
    }

    /* 사용자 ROLE 추출 */
    private static String extractOAuthRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        return role;
    }

    private void saveOrUpdateRefreshEntity(Member member, String newRefreshToken) {
        // 멤버의 PK 식별자로, refresh 토큰을 가져온다.
        Optional<Refresh> optionalRefresh = refreshRepository.findById(member.getId());
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);
        if (optionalRefresh.isPresent()) {
            // 로그인 이메일과 같은 이메일을 가지고 있는 Refresh 앤티티에 대해서, refresh 값을 새롭게 업데이트 해줌
            Refresh existedRefresh = optionalRefresh.get();
            // Dto 를 통해서, 새롭게 생성한 RefreshToken 값, 유효기간 등을 받아온다.
            RefreshDto refreshDto = RefreshDto.createRefreshDto(newRefreshToken, expirationDateTime);
            // Dto 정보들로 기존에 있던 Refresh 앤티티를 업데이트한다.
            existedRefresh.updateRefreshToken(refreshDto);
            // 저장
            refreshRepository.save(existedRefresh);
        } else {
            // 완전히 새로운 리프레쉬 토큰을 생성 후 저장.
            Refresh newRefreshEntity = new Refresh(member, newRefreshToken, expirationDateTime);
            refreshRepository.save(newRefreshEntity);
        }
    }

    private void addResponseDataV2(HttpServletResponse response, String accessToken, String refreshToken) throws IOException {
        // 엑세스 토큰을 JSON 형식으로 응답 데이터에 포함하여 클라이언트에게 반환
        JsonObject responseData = new JsonObject();
        responseData.addProperty("accessToken", accessToken);
        responseData.addProperty("refreshToken", refreshToken);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseData.toString());
        // HttpStatus 200 Ok
        response.setStatus(HttpStatus.OK.value());
        // 클라이언트 콘솔에 응답 로그 출력
        log.info("Response sent to client: " + responseData.toString());
    }
}

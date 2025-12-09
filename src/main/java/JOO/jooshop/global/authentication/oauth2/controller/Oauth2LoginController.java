package JOO.jooshop.global.authentication.oauth2.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.oauth2.custom.dto.KakaoProfile;
import JOO.jooshop.global.authentication.oauth2.custom.dto.OAuthToken;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.members.model.RefreshDto;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Social Login Controller
 * - 카카오/네이버 소셜 로그인 기능 담당
 * - 로그인 버튼 클릭 → OAuth2 인증 페이지 리다이렉트
 * - 인증 코드 수신 → AccessToken 발급 → 사용자 정보 조회 → 회원 가입/로그인 → JWT 발급
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/socialLogin")
public class Oauth2LoginController {

    /**
     * Oauth2LoginController
     *
     * 소셜 로그인 처리 흐름 (카카오, 네이버 등)
     *
     * 1. 클라이언트에서 소셜 로그인 버튼 클릭
     *      -> /api/socialLogin/authorization/{provider} 요청
     *
     * 2. 서버에서 OAuth2 인증 요청
     *      -> 소셜 로그인 인증 페이지(카카오/네이버)로 리다이렉트
     *
     * 3. 사용자 로그인 및 인증 동의
     *      -> 인증 성공 시, 소셜 서버에서 Authorization Code 발급
     *
     * 4. 서버에서 Authorization Code 수신
     *      -> /login/oauth2/code/{provider} 엔드포인트로 콜백
     *
     * 5. 서버에서 Access Token 요청
     *      -> Authorization Code + Client ID/Secret + Redirect URI 사용
     *      -> 소셜 API 호출 (RestTemplate)
     *
     * 6. Access Token으로 사용자 정보 조회
     *      -> 소셜 API 호출
     *      -> 사용자 이메일, 닉네임, 소셜 ID 획득
     *
     * 7. DB에서 소셜 회원 확인
     *      - 존재하면 기존 회원 정보 활성화 및 프로필 확인
     *      - 존재하지 않으면 회원 생성 및 프로필 생성
     *
     * 8. JWT AccessToken & RefreshToken 발급
     *      -> 클라이언트 응답으로 전달
     *      -> RefreshToken은 DB에 저장
     *
     * 9. 클라이언트에서 JWT 토큰으로 API 요청
     *      -> AccessToken 사용
     *      -> 만료 시 RefreshToken으로 재발급
     */


    private final MemberRepositoryV1 memberRepository;
    private final RefreshRepository refreshRepository;
    private final ProfileRepository profileRepository;
    private final JWTUtil jwtUtil;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    private final Long refreshTokenExpirationPeriod = 1209600L; // 14일

    // ===============================================
    // 1. 로그인 버튼 클릭 → 소셜 로그인 페이지로 리다이렉트
    // ===============================================

    @GetMapping("/authorization/kakao")
    public void kakaoLoginRedirect(HttpServletResponse response) throws IOException {
        String redirectUri = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + kakaoRedirectUri
                + "&response_type=code";
        response.sendRedirect(redirectUri);
    }

    @GetMapping("/authorization/naver")
    public void naverLoginRedirect(HttpServletResponse response) throws IOException {
        String state = "RANDOM_STATE_STRING"; // CSRF 방지용
        String redirectUrl = "https://nid.naver.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + naverClientId
                + "&redirect_uri=" + naverRedirectUri
                + "&state=" + state;
        response.sendRedirect(redirectUrl);
    }

    // ===============================================
    // 2. 콜백 처리 (카카오)
    // ===============================================
    @GetMapping("/login/oauth2/code/kakao")
    public void kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        OAuthToken oAuthToken = requestAccessTokenKakao(code);
        KakaoProfile kakaoProfile = requestKakaoProfile(oAuthToken.getAccessToken());
        handleSocialLogin(
                "kakao-" + kakaoProfile.getId(),
                kakaoProfile.getKakaoAccount().getEmail(),
                kakaoProfile.getProperties().getNickname(),
                SocialType.KAKAO,
                response
        );
    }

    // ===============================================
    // 3. 콜백 처리 (네이버)
    // ===============================================
    @GetMapping("/login/oauth2/code/naver")
    public void naverCallback(@RequestParam String code,
                              @RequestParam String state,
                              HttpServletResponse response) throws IOException {
        // TODO: 네이버 AccessToken 발급 & 사용자 정보 조회
        // 구조는 카카오와 동일 → handleSocialLogin 호출
    }

    // ===============================================
    // 4. 공통 로직: 소셜 로그인 처리
    // ===============================================
    private void handleSocialLogin(String socialId, String email, String username, SocialType socialType, HttpServletResponse response) throws IOException {
        Optional<Member> memberWithSocialId = memberRepository.findBySocialId(socialId);

        Member member;
        if (memberWithSocialId.isPresent()) {
            // 기존 회원
            member = memberWithSocialId.get();
            member.activate();
            if (profileRepository.findByMemberId(member.getId()).isEmpty()) {
                Profiles profile = Profiles.createMemberProfile(member);
                profileRepository.save(profile);
            }
        } else {
            // 신규 회원
            member = Member.createSocialMember(email, username, MemberRole.USER, socialType, socialId);
            member.activate();
            memberRepository.save(member);

            Profiles profile = Profiles.createMemberProfile(member);
            profileRepository.save(profile);
        }

        // JWT 발급
        String jwtAccessToken = jwtUtil.createAccessToken("access", member.getId().toString(), MemberRole.USER.toString());
        String jwtRefreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), MemberRole.USER.toString());

        addResponseData(response, jwtAccessToken, jwtRefreshToken, email);
        saveRefresh(member, jwtRefreshToken);
    }

    // ===============================================
    // 5. 카카오 AccessToken 발급
    // ===============================================
    private OAuthToken requestAccessTokenKakao(String code) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_url", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.getBody(), OAuthToken.class);
        } catch (JsonProcessingException e) {
            throw new IOException("카카오 AccessToken 요청 실패", e);
        }
    }

    // ===============================================
    // 6. 카카오 사용자 정보 조회
    // ===============================================
    private KakaoProfile requestKakaoProfile(String accessToken) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                request,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.getBody(), KakaoProfile.class);
        } catch (JsonProcessingException e) {
            throw new IOException("카카오 사용자 정보 조회 실패", e);
        }
    }

    // ===============================================
    // 7. JWT 응답 반환
    // ===============================================
    private void addResponseData(HttpServletResponse response, String accessToken, String refreshToken, String email) throws IOException {
        JsonObject responseData = new JsonObject();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        responseData.addProperty("accessToken", accessToken);
        responseData.addProperty("refreshToken", refreshToken);
        responseData.addProperty("email", email);

        response.getWriter().write(responseData.toString());
        response.setStatus(HttpStatus.OK.value());
    }

    // ===============================================
    // 8. RefreshToken DB 저장
    // ===============================================
    private void saveRefresh(Member member, String newRefreshToken) {
        Optional<Refresh> existedRefresh = refreshRepository.findByMember(member);
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);

        if (existedRefresh.isEmpty()) {
            Refresh refreshEntity = new Refresh(member, newRefreshToken, expirationDateTime);
            refreshRepository.save(refreshEntity);
        } else {
            Refresh refreshEntity = existedRefresh.get();
            RefreshDto refreshDto = RefreshDto.createRefreshDto(newRefreshToken, expirationDateTime);
            refreshEntity.updateRefreshToken(refreshDto);
            refreshRepository.save(refreshEntity);
        }
    }

    @Data
    public class OAuthClientDetails {
        private String clientId;
        private String clientSecret;
        private String tokenUri;
    }
}

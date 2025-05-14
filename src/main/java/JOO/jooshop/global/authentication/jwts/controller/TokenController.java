package JOO.jooshop.global.authentication.jwts.controller;


import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.authentication.jwts.utils.TokenResolver;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.model.RefreshDto;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import com.google.gson.JsonObject;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

import static JOO.jooshop.global.ResponseMessageConstants.REFRESH_NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/api/v1/reissue")
@RequiredArgsConstructor
public class TokenController {

    /*
    ※ JWT 클래스 정리
        1. TokenController
           - accessToken 재발급 : refresh 토큰 기반으로 access 재발급
           - 로그인 시, 토큰 발급 : access/refresh 클라이언트에게 제공
        2. TokenResolver
           - token 추출 : 요청에서 authorization header & cookie 에서 token 추출
        3. LoginFilter
           - 로그인 시, 사용자 정보를 확인하고 토큰을 발급하거나 검증하는 필터 클래스
        4. JWTFilterV3
           - access/refresh 검증 : JWT 토큰 검증 및 인증, access 만료 시, 새로운 access 발급

    ※ 전체 흐름
        1. 요청, 클라이언트 -> refreshToken cookie 포함해서 재발급 요청
        2. 쿠키 검사, refreshToken == null 유효성 검사
        3. JWT 검증, refreshToken
        4. 토큰 발급, 유효하면 accessToken 새로 발급
        5. 응답, 새 accessToken -> Authorization Header 에 실어서 응답
     */

    private JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final MemberRepositoryV1 memberRepository;
    private Long accessTokenExpirationPeriod = 60L * 30; // 30 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일

    @PostMapping("/access")
    public void reissueAccessToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 쿠키에서 refreshToken 가져오기
        Optional<String> refreshTokenOpt = TokenResolver.resolveTokenFromCookie(request, "refreshToken");

        if (refreshTokenOpt.isEmpty()) {
            sendLoginRequiredResponse(response, "unknown");
            return;
        }

        String refreshToken = refreshTokenOpt.get();

        // 2. 만료된 토큰 예외는 전역 예외 핸들러에서 처리하므로 따로 처리하지 않음
        jwtUtil.isExpired(refreshToken);

        // 3. MemberId와 Role 가져오기
        String memberId = jwtUtil.getMemberId(refreshToken);
        MemberRole role = jwtUtil.getRole(refreshToken);

        // 4. 새 Access Token 발급
        createAndSendNewAccessToken(response, memberId, role);
    }

    // 새 엑세스 토큰을 JSON으로 응답
    private void sendJsonResponseWithAccessToken(HttpServletResponse response, String newAccessToken) throws IOException {
        JsonObject responseData = new JsonObject();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        responseData.addProperty("accessToken", newAccessToken);
        response.getWriter().write(responseData.toString());
    }

    // 로그인 필요 응답 (401 Unauthorized)
    private void sendLoginRequiredResponse(HttpServletResponse response, String memberId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        log.info("Refresh expired!. memberId : {}", memberId);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("{\"message\":\"Please Login. Refresh expired!.\"}");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) throws NoSuchElementException, ClassNotFoundException {
        // 1. 쿠키로부터 RefreshToken 가져옴
        String refreshTokenInCookie = TokenResolver.resolveTokenFromCookie(request, "refreshToken")
                .orElseThrow(() -> new NoSuchElementException("Refresh 토큰이 존재하지 않습니다."));

        // 2. 토큰 유효성 검사 (만료 시 글로벌 핸들러가 처리)
        jwtUtil.isExpired(refreshTokenInCookie);

        // 3. 카테고리 검증
        String category = jwtUtil.getCategory(refreshTokenInCookie);
        if (!"refresh".equals(category)) {
            return ResponseEntity.badRequest().body("invalid refresh token. token의 category가 refresh가 아닙니다.");
        }

        // 4. DB 존재 여부 확인
        if (!refreshRepository.existsByRefreshToken(refreshTokenInCookie)) {
            throw new NoSuchElementException("Refresh 토큰이 존재하지 않습니다.");
        }

        // 5. 새 토큰 발급 및 응답 설정
        setResponseData(response, refreshTokenInCookie);
        return ResponseEntity.ok().build();
    }

    private void setResponseData(HttpServletResponse response, String refreshTokenInCookie) throws ClassNotFoundException {
        String memberId = jwtUtil.getMemberId(refreshTokenInCookie);
        MemberRole role = jwtUtil.getRole(refreshTokenInCookie);

        // 새 Access와 Refresh 토큰 발급
        String newAccess = jwtUtil.createAccessToken("access", memberId, role.toString());
        String newRefresh = jwtUtil.createRefreshToken("refresh", memberId, role.toString());

        // 기존 refreshToken 삭제하고 새 Refresh token DB에 저장
        refreshRepository.deleteByRefreshToken(refreshTokenInCookie);
        Member findMember = memberRepository.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new EntityNotFoundException("토큰 memberId에 해당하는 회원이 존재하지 않습니다."));

        saveRefreshEntity(findMember, newRefresh);

        // 새 Access Token을 Authorization 헤더에 설정하고 새 Refresh Token을 쿠키에 추가
        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refreshToken", newRefresh));
    }

    // 새 엑세스 토큰을 JSON으로 응답
    private void createAndSendNewAccessToken(HttpServletResponse response, String memberId, MemberRole role) throws IOException {
        String newAccessToken = jwtUtil.createAccessToken("access", memberId, role.toString());
        sendJsonResponseWithAccessToken(response, newAccessToken);
        log.info("New access token created. memberId : {}", memberId);
        response.setStatus(HttpStatus.OK.value());
    }

    // refresh entity 저장
    private void saveRefreshEntity(Member member, String refresh) throws ClassNotFoundException {
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);
        Refresh refreshEntity = refreshRepository.findById(member.getId())
                .orElseThrow(() -> new ClassNotFoundException("해당 Refresh가 존재하지 않습니다."));

        RefreshDto refreshDto = RefreshDto.createRefreshDto(refresh, expirationDateTime);
        refreshEntity.updateRefreshToken(refreshDto);
        refreshRepository.save(refreshEntity);
    }

    // 쿠키 생성
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 1일
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}

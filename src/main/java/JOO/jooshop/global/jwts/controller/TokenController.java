package JOO.jooshop.global.jwts.controller;


import JOO.jooshop.global.jwts.utils.CookieUtil;
import JOO.jooshop.global.jwts.utils.JWTUtil;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.ResponseMessageConstants.REFRESH_NOT_FOUND;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController {

    /*
    ※ TokenController class
        - 토큰 관련된 요청 처리 목적
        1. AccessToken 만료 -> 재발급 -> reissueAccessToken() 호출
        2. 최초 로그인 -> 토큰 발급 (로그인 성공 시, access/refresh 토큰을 header/cookie 에 실어줌)
        3. 로그아웃 -> refreshToken 삭제 (DB에 저장된 refreshToken 삭제 요청)
     */

    private JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final MemberRepositoryV1 memberRepository;
    private Long accessTokenExpirationPeriod = 60L * 30; // 30 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일

    /*
        ※ 전체 흐름
            1. 요청, 클라이언트 -> refershToken cookie 포함해서 재발급 요청
            2. 쿠키 검사, refreshToken == null 유효성 검사
            3. JWT 검증, refreshToken
            4. 토큰 발급, 유효하면 accessToken 새로 발급
            5. 응답, 새 accessToken -> Authorization Header 에 실어서 응답

        ※ 한 줄 정리:
            1. accessToken 만료 → 재발급 절차 진입
            2. refreshToken 확인 → 쿠키에서 가져와 검증 (위조, 만료 여부 체크)
            3. refreshToken 유효 → 새 accessToken + 새 refreshToken 발급 및 응답 (헤더 + 쿠키 설정)
            4. refreshToken도 만료 or 위조 → 재로그인 필요 (401 Unauthorized)
     */
    @ResponseBody
    @PostMapping("/api/v1/reissue/access")
    public void reissueAccessToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 2. 쿠키에서 리프레쉬 토큰 꺼내기
            String refreshAuthorization = CookieUtil.getCookieValue(request, "refreshAuthorization");

            // 3. 쿠키에 가져온 값이 "Bearer+"로 시작하는지 확인, 쿠키 값이 없거나 잘못된 경우 -> 로그인 요청 응답 반환
            if (refreshAuthorization == null || !refreshAuthorization.startsWith("Bearer+")) {
                sendLoginRequiredResponse(response, null);
                return;
            }
            // 접두사 제거해서 실제 리프레쉬 토큰만 꺼냄
            String refreshToken = refreshAuthorization.substring(7);

            // 4. JWT 토큰 유효성 검사, 유효하지 않으면 -> 로그인 요청 응답 반환
            if (!jwtUtil.validateToken(refreshToken)) {
                sendLoginRequiredResponse(response, null);
                return;
            }
            /*
                - 쿠키에서 가져온 리프레쉬 토큰이 DB에 존재하는지 체크한다. 정말 중요한 로직이다.
                  소셜로그인이나, 로그인을 진행하게 되면 새로운 리프레쉬 토큰을 DB에 저장하고,
                  로그아웃 시 DB 에 저장된 리프레쉬 토큰을 삭제하게 된다.
                  DB에 있는 리프레쉬 토큰을 조회한다는 것은 로그인을 진행 했는가?
                  로그아웃을 한 유저는 아닌가? 프로세스의 최종 관문이다.
             */
            // 5. DB에 저장된 리프레쉬 토큰 조회 (로그인 상태 확인)
            Refresh refreshTokenEntity = refreshRepository.findByRefreshToken(refreshToken).orElseThrow(
                    () -> new NoSuchElementException(REFRESH_NOT_FOUND));

            // 6. 리프레쉬 토큰 만료 시간 확인
            LocalDateTime refreshExpiration = refreshTokenEntity.getExpiration();
            if (refreshExpiration.isBefore(LocalDateTime.now())) {
                sendLoginRequiredResponse(response, String.valueOf(refreshTokenEntity.getMember().getId()));
                return;
            }

            // 7. JWT 토큰에서 사용자 정보 추출
            String memberId = jwtUtil.getMemberId(refreshToken);
            MemberRole role = jwtUtil.getRole(refreshToken);

            // 8. 새로운 액세스 토큰 생성
            String accessToken = jwtUtil.createAccessToken("access", memberId, String.valueOf(role));
            // 9. accessToken JSON 응답 반환 {"accessToken": "발급된 토큰"}
            sendJsonResponseWithAccessToken(response, accessToken);
            log.info("New access token created. memberId : " + memberId);
            response.setStatus(HttpURLConnection.HTTP_OK);
        } catch (Exception e) { // 10. 예외 처리
            log.error("Error during access token reissue", e);
            sendErrorResponse(response, "Failed to reissue access token");
        }
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

    // 서버 에러 응답 (500 Internal Server Error)
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.getWriter().write("{\"error\": \"" + errorMessage + "\"}");
    }

    @PostMapping("/api/v1/reissue/refresh")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) throws BadRequestException, ClassNotFoundException {
        /*
            ※ 전체 흐름
                1. 쿠키에서 refreshToken 가져옴
                2. refreshToken 유효성 검사 (expired)
                3. category = "refresh" 확인
                4. DB에 refreshToken 존재 여부 확인
                위 과정을 모두 거치면 (성공 시)
                      1. 새 accessToken 발급
                      2. 새 refreshToken 발급
                      3. DB에 새 refreshToken 저장
                      4. 응답에 accessToken, 쿠키에 refreshToken 설정

            ※ 핵심 포인트
                - 리프레쉬 토큰 관리 핵심
                    - 리프레쉬 토큰도 유효기간이 있음.
                    - 리프레쉬 토큰 재발급 시 DB 갱신 → 탈취 방지.
                    - 로그아웃 시 DB에서 리프레쉬 토큰 삭제 → 재사용 차단.
                - 토큰 발급 구조
                    - 액세스 토큰: 짧은 만료기간(예: 30분), 헤더로 전송.
                    - 리프레쉬 토큰: 긴 만료기간(예: 7일), 쿠키에 저장.
         */

        // 1. 쿠키로부터 RefreshToken 가져온다.
        String refreshTokenInCookie = getRefreshCookieValue(request); // 현재 사용 중인 리프레쉬 토큰
        if (refreshTokenInCookie == null || refreshTokenInCookie.isEmpty()) {
            throw new BadRequestException("Refresh token is missing or empty");
        }
        try { // 2. 토큰 유효성 검사
            jwtUtil.isExpired(refreshTokenInCookie);
        } catch (ExpiredJwtException e) {

            return ResponseEntity.badRequest().body("refresh token expired");
        }

        // 3. 토큰 카테고리 확인, 토큰 category claim = "refresh" 확인
        String category = jwtUtil.getCategory(refreshTokenInCookie);
        if (!category.equals("refresh")) {
            return ResponseEntity.badRequest().body("invalid refresh token. token의 category가 refresh가 아닙니다.");
        }

        // 4. DB에 리프레쉬 토큰 존재 여부 확인, 로그아웃 & 탈취된 토큰 차단 필수 로직
        if (!refreshRepository.existsByRefreshToken(refreshTokenInCookie)) {
            return ResponseEntity.badRequest().body("invalid refresh token. not exist refresh token");
        }

        // 5. 새로운 엑세스 토큰과 리스레쉬 토큰 발급
        setResponseData(response, refreshTokenInCookie);
        return ResponseEntity.ok().build();
    }

    private void handleRefreshExists(HttpServletResponse response, String memberId, MemberRole role, Refresh refresh) throws IOException {
        LocalDateTime expiration = refresh.getExpiration();

        if (expiration.isAfter(LocalDateTime.now())) {
            createAndSendNewAccessToken(response, memberId, role);
        } else {
            sendLoginRequiredResponse(response, memberId);
        }
    }

    private void createAndSendNewAccessToken(HttpServletResponse response, String memberId, MemberRole role) throws IOException {
        String newAccessToken = jwtUtil.createAccessToken("access", memberId, role.toString());

        sendJsonResponseWithAccessToken(response, newAccessToken);
        log.info("New access token created. memberId : {}", memberId);
        response.setStatus(HttpStatus.OK.value());
    }

    // 새로운 액세스 토큰 + 리프레쉬 토큰을 재발급하여 응답에 담는다.
    private void setResponseData(HttpServletResponse response, String refreshTokenInCookie) throws ClassNotFoundException {
        String memberId = jwtUtil.getMemberId(refreshTokenInCookie);
        MemberRole role = jwtUtil.getRole(refreshTokenInCookie);

        String newAccess = jwtUtil.createAccessToken("access", memberId, role.toString());
        String newRefresh = jwtUtil.createRefreshToken("refresh", memberId, role.toString());

        refreshRepository.deleteByRefreshToken(refreshTokenInCookie);
        Member findMember = memberRepository.findById(Long.valueOf(memberId)).orElseThrow(
                () -> new EntityNotFoundException("토큰 memberId에 해당하는 회원이 존재하지 않습니다."));

        // 새롭게 생성한 리프레쉬 토큰을 DB에 저장
        saveRefreshEntity(findMember, newRefresh);
        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refreshToken", newRefresh));
    }

    // 요청 쿠키에서 리프레쉬 토큰(refreshToken) 값을 가져온다.
    private String getRefreshCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("refreshToken"))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    // 회원의 리프레쉬 토큰 정보를 갱신해서 DB에 저장
    private void saveRefreshEntity(Member member, String refresh) throws ClassNotFoundException {
        // 현재 시간에 refreshTokenExpirationPeriod 을 더한 후, LocalDateTime으로 변환
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);

        Refresh refreshEntity = refreshRepository.findById(member.getId())
                .orElseThrow(() -> new ClassNotFoundException("해당 Refresh가 존재하지 않습니다."));
        // Dto 를 통해서, 새롭게 생성한 RefreshToken 값, 유효기간 등을 받아줍니다.
        RefreshDto refreshDto = RefreshDto.createRefreshDto(refresh, expirationDateTime);
        // Dto 정보들로 기존에 있던 Refresh 엔티티를 업데이트합니다.
        refreshEntity.updateRefreshToken(refreshDto);
        // 저장합니다.
        refreshRepository.save(refreshEntity);
    }

    // 리프레쉬 토큰 등을 담을 쿠키 생성.
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60 ); // 1일
        cookie.setHttpOnly(true);
//        cookie.setSecure(true); // HTTPS에서만 쿠키 전송
        cookie.setPath("/"); // 필요에 따라 설정
        return cookie;
    }

    // Authorization 헤더에서 액세스 토큰 추출.
    private String fetchTokenFromAuthorizationHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided and should start with 'Bearer '");
        }

        // Subtract 'Bearer ' part of token
        return bearerToken.substring(7);
    }
}

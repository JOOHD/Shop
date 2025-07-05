package JOO.jooshop.global.authentication.jwts.utils;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil { // JwtTokenProvider

    /*
        ※ JWTUtil 클래스 목적
             JWT Create (AccessToken / RefreshToken)
             JWT Validate (서명/만료 검증)
             JWT Parsing (memberId, category, role 추출)
             JWT Reissue (AccessToken 재발급)

        ※ 전체 흐름 (2025-06-13 리팩토링 반영)

        1. Spring 이 JWTUtil을 Bean 으로 등록하며,
           @PostConstruct 가 실행되어 application.yml 의 secretKey 값을 SecretKey 객체로 변환함.

        2. 로그인 성공 시,
           → createAccessToken(category, memberId, role) 을 호출하여 AccessToken 발급
           → createRefreshToken(category, memberId, role) 을 호출하여 RefreshToken 발급

        3. API 호출 시,
           → validateToken(token) 으로 JWT의 유효성 및 서명 검증

        4. JWT 추출 시,
           → parseToken(token) 을 통해 Claims 파싱
           → getMemberId(), getCategory(), getRole() 등으로 개별 claim 값 추출

        5. AccessToken 만료 여부는,
           → isExpired(token) 또는 getExpiration(token) 으로 확인 가능

        6. AccessToken 이 만료된 경우,
           → reissueAccessToken(refreshToken) 으로 새로운 AccessToken 재발급

        7. 로그아웃 처리 시,
           → getExpiration(token) 으로 블랙리스트 토큰의 만료 시간 추출
           → 해당 토큰을 Redis 블랙리스트에 저장하여 더 이상 사용하지 못하도록 차단
     */

    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    private static final String MEMBER_ID_KEY = "memberId";
    private static final String CATEGORY_KEY = "category";
    private static final String ROLE_KEY = "role";

    private final long accessTokenExpirationSeconds = 60L * 30;      // 30분
    private final long refreshTokenExpirationSeconds = 60L * 60 * 24 * 7; // 7일

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT SecretKey 로딩 완료 (base64).");
    }

    /** ======================== Token 생성 ======================== */

    public String createAccessToken(String category, String memberId, String role) {
        return createToken(category, memberId, role, accessTokenExpirationSeconds);
    }

    public String createRefreshToken(String category, String memberId, String role) {
        return createToken(category, memberId, role, refreshTokenExpirationSeconds);
    }

    private String createToken(String category, String memberId, String role, long expirationSeconds) {
        Date now = new Date();
        Date expiry = Date.from(LocalDateTime.now()
                .plusSeconds(expirationSeconds)
                .atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .claim(CATEGORY_KEY, category)
                .claim(MEMBER_ID_KEY, memberId)
                .claim(ROLE_KEY, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** ======================== Token 파싱 및 Claim 추출 ======================== */

    private Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 토큰이 비어 있습니다.");
        }
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    public String getMemberId(String token) {
        return parseToken(token).get(MEMBER_ID_KEY, String.class);
    }

    public String getCategory(String token) {
        return parseToken(token).get(CATEGORY_KEY, String.class);
    }

    public MemberRole getRole(String token) {
        return MemberRole.valueOf(parseToken(token).get(ROLE_KEY, String.class));
    }

    public String getId(String token) {
        return parseToken(token).getId();
    }

    public Date getExpiration(String accessToken) {
        return parseToken(accessToken).getExpiration();
    }

    /** ======================== Token 유효성 검사 ======================== */

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** ======================== AccessToken 재발급 ======================== */

    public String reissueAccessToken(String expiredToken) {
        Claims claims = parseToken(expiredToken);

        String category = claims.get(CATEGORY_KEY, String.class);
        String memberId = claims.get(MEMBER_ID_KEY, String.class);
        String role = claims.get(ROLE_KEY, String.class);

        return createAccessToken(category, memberId, role);
    }
}













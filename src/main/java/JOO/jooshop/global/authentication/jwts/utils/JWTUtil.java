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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {

    /*
    ※ JWTUtil 클래스 역할 요약

    - JWT 생성: AccessToken / RefreshToken / Email 인증 토큰 발급
    - JWT 검증: 서명 및 만료 여부 확인
    - JWT 파싱: memberId, category, role 등 Claim 추출
    - 토큰 재발급: 만료된 AccessToken → RefreshToken 기반으로 재발급

    ※ 전체 사용 흐름

    1. 초기화: @PostConstruct → secretKey 로딩 (application.yml)
    2. 로그인 성공 시: AccessToken, RefreshToken 생성
    3. API 호출 시: validateToken() 으로 토큰 유효성 검증
    4. 토큰 파싱: getMemberId(), getCategory(), getRole() 등 Claim 추출
    5. 만료 확인: isExpired(), getExpiration()
    6. 재발급: reissueAccessToken(refreshToken)
    7. 로그아웃: getExpiration() → Redis 블랙리스트 저장
    */

    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    private static final String MEMBER_ID_KEY = "memberId";
    private static final String CATEGORY_KEY = "category";
    private static final String ROLE_KEY = "role";

    private final long accessTokenExpirationSeconds = 60L * 30;      // 30분
    private final long refreshTokenExpirationSeconds = 60L * 60 * 24 * 7; // 7일

    /**
     * Bean 초기화 시 실행됨.
     * Base64 인코딩된 문자열을 SecretKey 객체로 변환하여 JWT 서명에 사용
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT SecretKey 로딩 완료 (base64).");
    }

    /**
     * AccessToken 생성 메서드
     */
    public String createAccessToken(String category, String memberId, String role) {
        return createToken(category, memberId, role, accessTokenExpirationSeconds);
    }

    /**
     * RefreshToken 생성 메서드
     */
    public String createRefreshToken(String category, String memberId, String role) {
        return createToken(category, memberId, role, refreshTokenExpirationSeconds);
    }

    /**
     * Access/Refresh 공통 토큰 생성 메서드
     * - category, memberId, role 클레임을 포함
     */
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

    /**
     * 이메일 인증 전용 토큰 생성 (만료 시간: 15분)
     */
    public String createEmailToken(String email) {
        long emailTokenExpireSeconds = 60L * 15; // 15분

        Date now = new Date();
        Date expiry = Date.from(LocalDateTime.now()
                .plusSeconds(emailTokenExpireSeconds)
                .atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * JWT 문자열에서 Claims(클레임 정보)를 파싱함
     */
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

    /**
     * JWT memberId 클레임 추출
     */
    public String getMemberId(String token) {
        return parseToken(token).get(MEMBER_ID_KEY, String.class);
    }

    /**
     * JWT category 클레임 추출
     */
    public String getCategory(String token) {
        return parseToken(token).get(CATEGORY_KEY, String.class);
    }

    /**
     * JWT role 클레임 추출 및 MemberRole enum으로 변환
     */
    public MemberRole getRole(String token) {
        return MemberRole.valueOf(parseToken(token).get(ROLE_KEY, String.class));
    }

    /**
     * JWT ID 필드(jti) 추출
     */
    public String getId(String token) {
        return parseToken(token).getId();
    }

    /**
     * JWT의 만료 시간(Expiration) 추출
     */
    public Date getExpiration(String accessToken) {
        return parseToken(accessToken).getExpiration();
    }

    /**
     * 이메일 인증 토큰에서 이메일 주소 추출
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    /**
     * JWT 토큰의 서명 및 만료 여부를 검증
     * 유효한 토큰이면 true, 아니면 false
     */
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

    /**
     * JWT 토큰이 만료되었는지 확인
     */
    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * RefreshToken 기반으로 새로운 AccessToken 재발급
     */
    public String reissueAccessToken(String expiredToken) {
        Claims claims = parseToken(expiredToken);

        String category = claims.get(CATEGORY_KEY, String.class);
        String memberId = claims.get(MEMBER_ID_KEY, String.class);
        String role = claims.get(ROLE_KEY, String.class);

        return createAccessToken(category, memberId, role);
    }
}

package JOO.jooshop.global.jwts.utils;

import JOO.jooshop.members.entity.enums.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
        ※ 전체 흐름
        1. Spring 이 JWTUtil 을 빈으로 등록하면서 @PostConstruct 실행 → SecretKey 초기화
        2. 로그인 성공 시, createAccessToken & createRefreshToken 으로 JWT 발급
        3. API 호출 시 validateToken()을 통해 JWT 유효성 검사
        4. JWT 에서 memberId, category, role 을 추출하여 사용자 정보 확인
        5. isExpired()로 AccessToken 만료 여부 확인
        6. 만료된 AccessToken 이면 parseAccessToken()을 이용해 새로운 AccessToken 재발급
     */

    private SecretKey secretKey; // 'final' 제거
    @Value("${spring.jwt.secret}")
    private String jwtSecret;
    private static final String MEMBERPK_CLAIM_KEY = "memberId";
    private static final String CATEGORY_CLAIM_KEY = "category";
    private Long accessTokenExpirationPeriod = 60L * 12; // 12 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일

//    public JWTUtil() {
//        this.secretKey = Jwts.SIG.HS256.key().build();
//    }

    @PostConstruct
    public void init() { // @Value로 주입된 jwtSecret 값을 SecretKey로 변환, JWTUtil을 생성때 한 번 실행됨.
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes); // 이제 JWT를 서명(Sign)할 수 있다.
    }

    private Claims parseToken(String token) {
        // Check if token is null or empty
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new RuntimeException("Filed to parse JWT token", e);
        }
    }

    public String getMemberId(String token) {
        return parseToken(token).get(MEMBERPK_CLAIM_KEY, String.class);
    }

    public String getCategory(String token) {
        return parseToken(token).get(CATEGORY_CLAIM_KEY, String.class);
    }

    public MemberRole getRole(String token) {
        return MemberRole.valueOf(parseToken(token).get("role", String.class));
    }

    public Boolean isExpired(String token) {
        // Check if token is null or empty
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        // Validate token structure
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token is not valid");
        }

        // Check expiration
        return parseToken(token).getExpiration().before(new Date());
    }

    private String createToken(String category, String memberId, String role, Date expirationDate) {
        return Jwts.builder()
                .claim(CATEGORY_CLAIM_KEY, category)
                .claim(MEMBERPK_CLAIM_KEY, memberId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis())) // 재발급 (refreshToken)
                .expiration(expirationDate)
                .signWith(secretKey, Jwts.SIG.HS256) // postman basic HS256
                .compact();
    }

    public String createAccessToken(String category, String memberId, String role) {
        // 30분
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(accessTokenExpirationPeriod);
        Date expirationDate = Date.from(expirationDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return createToken(category, memberId, role, expirationDate);
    }

    public String createRefreshToken(String category, String memberId, String role) {
        // 7 일
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationPeriod);
        Date expirationDate = Date.from(expirationDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return createToken(category, memberId, role, expirationDate);
    }

    // 액세스 토큰 파싱 후, 토큰형태로 반환합니다.
    public String parseAccessToken(String accessToken) {
        Claims claims = parseToken(accessToken);

        // 토큰에서 category, memberId, role을 추출
        String category = claims.get(CATEGORY_CLAIM_KEY, String.class);
        String memberId = claims.get(MEMBERPK_CLAIM_KEY, String.class);
        String role = claims.get("role", String.class);

        // AccessToken의 만료 시간을 가져옴
        Date expirationDate = claims.getExpiration();

        // 새로운 AccessToken 생성
        return createToken(category, memberId, role, expirationDate);
    }

    /**
     * 토큰 유효성 체크
     *
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.error("JWT 유효성 검사에 실패했습니다.", e);
        }
        return false;
    }
}



















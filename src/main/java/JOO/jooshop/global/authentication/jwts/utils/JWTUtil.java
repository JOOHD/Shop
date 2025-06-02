package JOO.jooshop.global.authentication.jwts.utils;

import JOO.jooshop.members.entity.enums.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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
public class JWTUtil { // JwtTokenProvider

    /*
        ※ JWTUtil 클래스 목적
             JWT Create, AccessToken/RefreshToken
             JWT Validate, expiration, reissue
             JWT Parsing, claims(memberId, category, role)

        ※ 전체 흐름
        1. Spring 이 JWTUtil, Spring Bean 등록,
            @PostConstruct 실행, application.yml 의 jwtSecret -> SecretKey 변환
        2. 로그인 성공 시, createAccess/RefreshToken 으로 JWT 발급
        3. API 호출 시 validateToken()을 통해 JWT 유효성 검사
        4. parseToken(token), claims(memberId, category, role) 을 추출, 사용자 정보 확인
        5. isExpired()로 AccessToken 만료 여부 확인,
        6. 만료된 AccessToken 이면 reissueAccessToken()을 이용해 새로운 AccessToken 재발급
     */

    private SecretKey secretKey;   // 'final' 제거
    @Value("${spring.jwt.secret}") // 문자열로 주입받음
    private String jwtSecret;
    private static final String MEMBERPK_CLAIM_KEY = "memberId";
    private static final String CATEGORY_CLAIM_KEY = "category";
    private Long accessTokenExpirationPeriod = 60L * 30; // 30 분
    private Long refreshTokenExpirationPeriod = 3600L * 24 * 7; // 7일

    @PostConstruct
    public void init() { // @Value로 주입된 jwtSecret 값을 SecretKey로 변환, JWTUtil을 생성때 한 번 실행됨.
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            this.secretKey = Jwts.SIG.HS256.key().build();
            log.info("JWT SecretKey 자동 생성 완료");
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
                this.secretKey = Keys.hmacShaKeyFor(keyBytes);
                log.info("JWT SecretKey (yml 설정값 사용) 적용 완료");
            } catch (IllegalArgumentException e) {
                log.error("Base64 디코딩 실패: jwt.secret 값을 확인하세요", e);
                throw e; // 혹은 CustomException 던져도 됨
            }
        }
    }

    private Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("parseToken: 전달받은 토큰이 null 이거나 비어 있습니다.");
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            log.debug("parseToken: 토큰 파싱 시도 - {}", token);
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            log.info("parseToken: 토큰 파싱 성공 - subject: {}, expiration: {}",
                    claims.getSubject(), claims.getExpiration());
            return claims;
        } catch (SignatureException e) { // SignatureException 추가
            log.error("JWT 서명이 유효하지 않습니다.", e);
            throw new IllegalArgumentException("잘못된 JWT 서명입니다.");
        } catch (JwtException e) {
            log.error("JWT 파싱에 실패했습니다.", e);
            throw new IllegalArgumentException("JWT 파싱 실패: 유효하지 않은 토큰입니다.");
        }
    }

    public String getMemberId(String token) {
        Object memberId = parseToken(token).get(MEMBERPK_CLAIM_KEY); // "memberId"는 Long 일 수도 있음
        return memberId != null ? memberId.toString() : null;
    }

    public String getCategory(String token) {
        Object category = parseToken(token).get(CATEGORY_CLAIM_KEY); // NPE 을 피하면서도 memberId의 값을 안전하게 확인
        return category != null ? category.toString() : null;
    }

    public MemberRole getRole(String token) {
        return MemberRole.valueOf(parseToken(token).get("role", String.class));
    }

    // getId() - JWT ID (jti)를 반환, 클레임을 추출하여 Redis 블랙리스트 체크에 사용
    public String getId(String token) {
        return parseToken(token).getId();
    }

    // getExpiration() - 토큰의 만료 시간 확인에 사용 (Date 타입 반환)
    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
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
    public String reissueAccessToken(String expiredAccessToken) {
        Claims claims = parseToken(expiredAccessToken);

        // 토큰에서 category, memberId, role을 추출
        String category = claims.get(CATEGORY_CLAIM_KEY, String.class);
        String memberId = claims.get(MEMBERPK_CLAIM_KEY, String.class);
        String role = claims.get("role", String.class);

        // 기존 AccessToken 의 만료 시간을 가져옴
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
        if (token == null || token.trim().isEmpty()) {
            log.warn("validateToken: 전달받은 토큰이 null 이거나 비어 있습니다.");
            return false;
        }

        try {
            log.debug("validateToken: 토큰 유효성 검사 시도 - {}", token);
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            log.info("validateToken: 토큰 유효성 검사 통과");
            return true;
        } catch (SignatureException e) {
            log.error("validateToken: JWT 서명이 유효하지 않습니다. token: {}", token, e);
        } catch (JwtException e) {
            log.error("validateToken: JWT 유효성 검사 실패. token: {}", token, e);
        }
        return false;
    }
}



















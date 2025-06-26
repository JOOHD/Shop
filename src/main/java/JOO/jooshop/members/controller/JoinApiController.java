package JOO.jooshop.members.controller;

import JOO.jooshop.global.Exception.InvalidCredentialsException;
import JOO.jooshop.global.Exception.UnverifiedEmailException;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RedisRefreshTokenRepository;
import JOO.jooshop.members.service.MemberService;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Transactional
@RequestMapping("/api")
public class JoinApiController {

    /*
        ※ JoinApiController 클래스 목적
             - 이메일/비밀번호 로그인 → JWT 발급 + AccessToken 쿠키 설정
             - OAuth2 로그인 → success handler 처리 별도, 여기선 X
             - 로그아웃 시 쿠키 삭제 및 AccessToken 블랙리스트 처리

        ※ 전체 흐름
        1. [POST] /login 요청 수신 (login.html에서 fetch로 호출)
        2. 이메일/비밀번호를 받아 회원 인증
        3. 인증 성공 시 AccessToken/RefreshToken 생성
        4. AccessToken은 HttpOnly + Secure 쿠키로 응답에 포함
        5. RefreshToken은 Redis에 저장 (쿠키 X, 보안 강화를 위해)
        6. 인증 실패 시 적절한 상태코드와 메시지 반환
     */

    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final EmailMemberService emailMemberService;
    private final MemberRepositoryV1 memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${IMP_SECRET_KEY}")
    private String secretKey;

    @PostMapping("/join") // 회원가입
    public ResponseEntity<?> join(@RequestBody @Valid JoinMemberRequest request) {
        if (isInvalidNickname(request.getNickname())) {
            return ResponseEntity.badRequest().body("닉네임을 입력해야 합니다.");
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        }

        String token = UUID.randomUUID().toString();
        Member member = createMemberFromRequest(request, token, false);
        Member joinMember = memberService.joinMember(member);
        profileRepository.save(Profiles.createMemberProfile(joinMember));

        sendVerificationEmail(joinMember.getEmail(), token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new JoinMemberResponse(joinMember.getId(), joinMember.getEmail()));
    }

    @PostMapping("/admin/join") // 관리자 회원가입
    public ResponseEntity<?> joinAdmin(@RequestBody @Valid JoinMemberRequest request) {
        if (isInvalidNickname(request.getNickname())) {
            return ResponseEntity.badRequest().body("Nickname cannot be empty");
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        String token = UUID.randomUUID().toString();
        Member admin = createMemberFromRequest(request, token, true);
        admin.isAdmin();
        admin.activate();

        Member savedAdmin = memberRepository.save(admin);
        profileRepository.save(Profiles.createMemberProfile(savedAdmin));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new JoinMemberResponse(savedAdmin.getId(), savedAdmin.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<String> oauth2Login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        try {
            // 1. 사용자 인증
            Member member = memberService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

            // 2. 토큰 생성
            String accessToken = jwtUtil.createAccessToken("access", member.getId().toString(), member.getRole().name());
            String refreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), member.getRole().name());

            // 3. RefreshToken 저장 (Redis)
            redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);

            // 4. AccessToken 쿠키로 설정
            Cookie jwtCookie = new Cookie("accessToken", accessToken);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);  // https 환경에서만 전송
            jwtCookie.setPath("/");

            // 현재 시간 기준으로 남은 초 단위 시간 적용
            Date expiration = jwtUtil.getExpiration(accessToken);
            int maxAge = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
            jwtCookie.setMaxAge(maxAge);

            response.addCookie(jwtCookie);

            return ResponseEntity.ok("로그인 성공, 환영합니다.");

        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일 또는 비밀번호가 틀렸습니다.");
        } catch (UnverifiedEmailException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("인증되지 않은 이메일입니다.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "accessToken", required = false) String accessTokenCookie,
                                    @CookieValue(name = "refreshAuthorization", required = false) String refreshTokenCookie,
                                    HttpServletResponse response) {
        if (accessTokenCookie == null || refreshTokenCookie == null) {
            return ResponseEntity.badRequest().body("이미 로그아웃된 회원입니다.");
        }

        try {
            // Bearer 접두사 제거
            String accessToken = accessTokenCookie.startsWith("Bearer ")
                    ? accessTokenCookie.substring(7)
                    : accessTokenCookie;

            // 유효성 검사
            if (!jwtUtil.validateToken(accessToken)) {
                log.warn("[Logout] 유효하지 않은 AccessToken");
            } else {
                // Redis에서 저장된 Refresh Token 삭제
                String memberId = jwtUtil.getMemberId(accessToken);
                String redisKey = "refresh:" + memberId;
                if (redisTemplate.hasKey(redisKey)) {
                    redisTemplate.delete(redisKey);
                    log.info("[Logout] Redis에서 Refresh Token 삭제 완료: {}", redisKey);
                }

                // 만료 시간 구하기
                Date expirationDate = jwtUtil.getExpiration(accessToken);
                long now = System.currentTimeMillis();
                long remainingSeconds = (expirationDate.getTime() - now) / 1000L;

                // 블랙리스트 등록
                redisTemplate.opsForValue().set(
                        "blacklist:" + accessToken,
                        "logout",
                        Duration.ofSeconds(remainingSeconds)
                );

                log.info("[Logout] AccessToken 블랙리스트 등록 완료 (memberId: {})", memberId);
            }

            // 쿠키 삭제
            Cookie deleteAccessToken = new Cookie("accessToken", null);
            deleteAccessToken.setMaxAge(0);
            deleteAccessToken.setPath("/");
            response.addCookie(deleteAccessToken);

            Cookie deleteRefreshToken = new Cookie("refreshAuthorization", null);
            deleteRefreshToken.setMaxAge(0);
            deleteRefreshToken.setPath("/");
            response.addCookie(deleteRefreshToken);

            return ResponseEntity.ok("로그아웃 되었습니다.");

        } catch (Exception e) {
            log.error("Logout 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("로그아웃 처리 중 오류가 발생했습니다.");
        }
    }

    private Member createMemberFromRequest(JoinMemberRequest request, String token, boolean isAdmin) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String socialId = "general-" + uuid.substring(0, 12);

        return isAdmin
                ? Member.createAdminMember(request.getEmail(), request.getNickname(),
                passwordEncoder.encode(request.getPassword1()), token, socialId)
                : Member.createGeneralMember(request.getEmail(), request.getNickname(),
                passwordEncoder.encode(request.getPassword1()), token, socialId);
    }

    private boolean isInvalidNickname(String nickname) {
        return nickname == null || nickname.trim().isEmpty();
    }

    private void sendVerificationEmail(String email, String token) {
        try {
            Member member = Member.createEmailMember(email, token);
            emailMemberService.sendEmailVerification(member);
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
        }
    }

    @Data
    private static class JoinMemberRequest {
        @NotBlank
        private String email;
        @NotBlank
        private String nickname;
        @NotBlank
        private String password1;
        @NotBlank
        private String password2;
    }

    @Data
    private static class JoinMemberResponse {
        private Long memberId;
        private String email;

        public JoinMemberResponse(Long memberId, String email) {
            this.memberId = memberId;
            this.email = email;
        }
    }
}


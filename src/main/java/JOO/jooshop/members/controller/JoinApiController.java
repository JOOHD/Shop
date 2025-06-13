package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
public class JoinApiController {

    /*
        ※ JoinApiController 클래스 목적
             회원가입 및 로그인 처리
             JWT 기반 토큰 생성 및 응답 쿠키 설정
             RefreshToken Redis 저장 처리 포함

        ※ 전체 흐름 (2025-06-13 리팩토링 반영)

        1. 회원가입 요청 (signUp)
           - 이메일 중복 확인
           - 패스워드 암호화 후 회원 저장

        2. 로그인 요청 (signIn)
           - 이메일/비밀번호 인증
           - AccessToken / RefreshToken 생성
             → JWTUtil.createAccessToken(), createRefreshToken()
           - AccessToken: HttpOnly + Secure 쿠키에 저장
           - RefreshToken: Redis 에 저장 (key=memberId, value=token)

        3. 로그아웃 요청 (signOut)
           - AccessToken 파싱하여 만료시간 추출
           - Redis 블랙리스트에 저장하여 무효 처리
           - AccessToken / RefreshToken 쿠키 제거

        ※ 리팩토링 핵심
           - JWTUtil 메서드명 일관성 적용 (createAccessToken 등)
           - 쿠키명, 쿠키 설정 HttpOnly/Secure 일괄 정리
           - 중복 코드 제거 및 메서드 분리 (e.g., 쿠키 생성 로직 등)
     */

    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final EmailMemberService emailMemberService;
    private final MemberRepositoryV1 memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${IMP_SECRET_KEY}")
    private String secretKey;

    @PostMapping("/join") // 회원가입
    public ResponseEntity<?> joinMemberV1(@RequestBody @Valid JoinMemberRequest request) {
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
        admin.verifyAdminUser();
        admin.activateMember();

        Member savedAdmin = memberRepository.save(admin);
        profileRepository.save(Profiles.createMemberProfile(savedAdmin));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new JoinMemberResponse(savedAdmin.getId(), savedAdmin.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response)
            throws UserPrincipalNotFoundException, CredentialNotFoundException {
        Member member = memberService.memberLogin(loginRequest);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        if (!member.isCertifyByMail()) {
            return ResponseEntity.badRequest().body("이메일 인증이 되지 않은 회원입니다.");
        }

        // JWT 발급 (리팩토링된 메서드 사용)
        String accessToken = jwtUtil.createAccessToken(
                "access",                   // category
                String.valueOf(member.getId()),     // memberId
                member.getMemberRole().name()       // role
        );

        Cookie jwtCookie = new Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60);
        response.addCookie(jwtCookie);

        return ResponseEntity.ok("로그인 성공, 환영합니다.");
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
                passwordEncoder.encode(request.getPassword()), token, socialId)
                : Member.createGeneralMember(request.getEmail(), request.getNickname(),
                passwordEncoder.encode(request.getPassword()), token, socialId);
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
        private String email;
        private String nickname;
        private String password;
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


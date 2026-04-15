package JOO.jooshop.members.service;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.exception.customException.UnverifiedEmailException;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.request.LoginRequest;
import JOO.jooshop.members.repository.MemberRepository;
import JOO.jooshop.members.repository.RedisRefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final MemberRepository memberRepository;

    /**
     [Service]
     * 기존
     * - MemberService 하나에 모든 책임 집중
        (회원가입 / 로그인 / 인증 / 프로필 / 비밀번호 변경 등)
     * - 변경 이유가 많아지는 God Service 형태
     * - 인증 흐름과 회원 관리 흐름이 강하게 결합
     *
     * refactoring 26.04
     * - MemberAuthService (로그인/회원가입/인증)
     * - MemberAccountService (프로필/비밀번호/조회)
     * - 책임 분리로 변경 영향 범위 최소화
     * - 엔티티 상태 변경은 도메인 메서드 호출로 처리
     */

    @Transactional
    public String login(LoginRequest loginRequest, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일이 올바르지 않습니다."));

        validateLoginPassword(loginRequest.getPassword(), member.getPassword());
        validateLoginAvailable(member);

        String accessToken = jwtUtil.createAccessToken(
                "access",
                member.getId().toString(),
                member.getMemberRole().name()
        );

        String refreshToken = jwtUtil.createRefreshToken(
                "refresh",
                member.getId().toString(),
                member.getMemberRole().name()
        );

        redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);
        addAccessTokenCookie(response, accessToken);

        return "로그인 성공, 환영합니다.";
    }

    public void validateLoginAvailable(Member member) {
        if (!member.isCertifiedByEmail()) {
            throw new UnverifiedEmailException("인증되지 않은 이메일입니다.");
        }

        if (!member.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        if (member.isBanned()) {
            throw new IllegalStateException("정지된 계정입니다.");
        }

        if (member.isAccountExpired()) {
            throw new IllegalStateException("만료된 계정입니다.");
        }

        if (member.isPasswordExpired()) {
            throw new IllegalStateException("비밀번호가 만료되었습니다.");
        }
    }

    private void validateLoginPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }
    }

    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie jwtCookie = new Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");

        int maxAge = (int) ((jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000);
        jwtCookie.setMaxAge(maxAge);

        response.addCookie(jwtCookie);
    }
}
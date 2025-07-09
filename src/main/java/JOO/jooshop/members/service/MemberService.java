package JOO.jooshop.members.service;

import JOO.jooshop.global.Exception.customException.ExistingMemberException;
import JOO.jooshop.global.Exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.Exception.customException.MemberNotFoundException;
import JOO.jooshop.global.Exception.customException.UnverifiedEmailException;
import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.JoinMemberRequest;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RedisRefreshTokenRepository;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepositoryV1 memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final ProfileRepository profileRepository;
    private final EmailMemberService emailMemberService;

    @Transactional
    public Member registerMember(JoinMemberRequest request) {
        validateDuplicateEmail(request.getEmail());

        String token = UUID.randomUUID().toString();
        String socialId = generateSocialId();

        Member member = Member.createGeneralMember(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword1()),
                request.getPhone(),
                socialId);

        memberRepository.save(member);
        profileRepository.save(Profiles.createMemberProfile(member));
        sendVerificationEmail(member.getEmail());

        return member;
    }

    @Transactional
    public Member registerAdmin(JoinMemberRequest request) {
        validateDuplicateEmail(request.getEmail());

        String token = UUID.randomUUID().toString();
        String socialId = generateSocialId();

        Member admin = Member.createAdminMember(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword1()),
                request.getPhone(),
                socialId);

        admin.activate();
        memberRepository.save(admin);
        profileRepository.save(Profiles.createMemberProfile(admin));
        return admin;
    }

    @Transactional
    public String login(LoginRequest loginRequest, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일이 올바르지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        if (!member.isCertifiedByEmail()) {
            throw new UnverifiedEmailException("인증되지 않은 이메일입니다.");
        }

        String accessToken = jwtUtil.createAccessToken("access", member.getId().toString(), member.getMemberRole().name());
        String refreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), member.getMemberRole().name());

        redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);

        Cookie jwtCookie = new Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        int maxAge = (int) ((jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000);
        jwtCookie.setMaxAge(maxAge);
        response.addCookie(jwtCookie);

        return "로그인 성공, 환영합니다.";
    }

    private void sendVerificationEmail(String email) {
        try {
            // Member 객체 생성 없이 바로 email 전달
            emailMemberService.sendEmailVerification(email);
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
        }
    }

    public void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ExistingMemberException(email);
        }
    }

    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException(email));
    }

    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("해당 ID로 사용자를 찾을 수 없습니다: " + id));
    }

    private String generateSocialId() {
        return "general-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

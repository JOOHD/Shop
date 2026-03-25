package JOO.jooshop.members.service;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.address.model.AddressesReqeustDto;
import JOO.jooshop.address.repository.AddressRepository;
import JOO.jooshop.global.exception.customException.ExistingMemberException;
import JOO.jooshop.global.exception.customException.InvalidCredentialsException;
import JOO.jooshop.global.exception.customException.MemberNotFoundException;
import JOO.jooshop.global.exception.customException.UnverifiedEmailException;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisRefreshTokenRepository redisRefreshTokenRepository;
    private final EmailMemberService emailMemberService;
    private final MemberRepositoryV1 memberRepository;
    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;

    /**
     * 26.03.25 refactoring
     * 1. 이메일 중복 체크
     * 2. 비밀번호 검증
     * 3. Member Root 생성
     * 4. 기본 Profile child 생성 및 편입
     * 5. Member 저장 (cascade로 profile 함께 저장)
     * 6. 주소 등록 (현재는 aggregate 바깥 협력 객체로 유지)
     * 7. 인증 메일 발송
     *
     * 핵심
     * - Root 가 child 를 편입
     *
     * Member member = Member.registerGeneral(...);
     * Profiles defaultProfile = Profiles.createDefaultProfile();
     * member.attachProfile(defaultProfile);
     * Member savedMember = memberRepository.save(member);
     */

    @Transactional
    public Member registerMember(JoinMemberRequest request) {
        validateDuplicateEmail(request.getEmail());
        validatePasswordMatch(request.getPassword1(), request.getPassword2());

        // 랜덤 소셜ID 생성
        String socialId = generateSocialId();

        // 기본 회원 생성 (정상 회원)
        Member member = Member.registerGeneral(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword1()), // 비밀번호 암호화
                request.getUsername(),
                request.getNickname(),
                request.getPhoneNumber(),
                socialId
        );

        Profiles profile = Profiles.createDefaultProfile();

        member.attachProfile(profile);

        Member savedMember = memberRepository.save(member);

        sendVerificationEmail(savedMember.getEmail());

        return savedMember;
    }


    /**
     * 로그인
     * 1. 이메일 조회
     * 2. 비밀번호 체크
     * 3. 이메일 인증 여부 확인
     * 4. AccessToken + RefreshToken 발급
     * 5. AccessToken은 HttpOnly + Secure 쿠키 저장
     * 6. RefreshToken은 Redis 저장
     */
    @Transactional(readOnly = true)
    public String login(LoginRequest loginRequest, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일이 올바르지 않습니다."));

        validateLoginPassword(loginRequest.getPassword(), member.getPassword());

        if (!member.isCertifiedByEmail()) {
            throw new UnverifiedEmailException("인증되지 않은 이메일입니다.");
        }

        // JWT 발급
        String accessToken = jwtUtil.createAccessToken("access", member.getId().toString(), member.getMemberRole().name());
        String refreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), member.getMemberRole().name());

        // RefreshToken → Redis 저장
        redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);
        addAccessTokenCookie(response, accessToken);

        return "로그인 성공, 환영합니다.";
    }

    /**
     * 안전한 비밀번호 변경
     * 1. 기존 비밀번호 확인
     * 2. 새 비밀번호/확인값 체크
     * 3. 암호화하여 저장
     */
    @Transactional
    public void resetPassword(Long memberId, String currentPassword, String newPassword, String newPasswordConfirm) {
        Member member = findMemberById(memberId);

        validateCurrentPassword(currentPassword, member.getPassword());
        validatePasswordMatch(newPassword, newPasswordConfirm);

        member.changePassword(passwordEncoder.encode(newPassword));

        log.info("회원 {} 비밀번호가 성공적으로 변경되었습니다.", member.getEmail());
    }

    @Transactional
    public void verifyEmail(Long memberId) {
        Member member = findMemberById(memberId);
        member.verifyEmail();
    }

    /**
     * 이메일 중복 체크
     * - 회원가입 시 반드시 호출
     */
    public void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ExistingMemberException(email);
        }
    }

    /**
     * 이메일로 회원 조회
     * - 로그인/조회 로직에서 사용
     */
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException(email));
    }

    /**
     * ID로 회원 조회
     * - 프로필 조회 / 비밀번호 변경 등에 사용
     */
    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("해당 ID로 사용자를 찾을 수 없습니다: " + id));
    }

    private void validatePasswordMatch(String password1, String password2) {
        if (!password1.equals(password2)) {
            throw new InvalidCredentialsException("비밀번호가 서로 일치하지 않습니다.");
        }
    }

    private void validateCurrentPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException("기존 비밀번호가 일치하지 않습니다.");
        }
    }

    private void validateLoginPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }
    }

    private void registerAddressIfExists(AddressesReqeustDto addressDto, Member member) {
        if (addressDto == null) {
            return;
        }

        addressRepository.resetDefaultAddressForMember(member.getId());

        Addresses newAddress = Addresses.createAddress(addressDto, member);
        newAddress.setDefaultAddress(true);
        addressRepository.save(newAddress);
    }

    /**
     * 인증 이메일 발송
     * - 예외 발생해도 회원가입 흐름을 막지 않기 위해 try-catch 처리
     */
    private void sendVerificationEmail(String email) {
        try {
            emailMemberService.sendEmailVerification(email);
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
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

    /**
     * 랜덤 소셜 ID 생성
     * - 일반 회원도 내부적으로 고유한 소셜 ID 생성하여 저장
     */
    private String generateSocialId() {
        return "general-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

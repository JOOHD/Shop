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
     * 일반 회원 가입
     * 1. 이메일 중복 체크
     * 2. Member 엔티티 생성
     * 3. 프로필 자동 생성
     * 4. 기본 주소 등록
     * 5. 이메일 인증 메일 발송
     */
    @Transactional
    public Member registerMember(JoinMemberRequest request) {
        validateDuplicateEmail(request.getEmail());

        // 랜덤 소셜ID 생성
        String socialId = generateSocialId();

        // 기본 회원 생성 (정상 회원)
        Member member = Member.createGeneralMember(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword1()), // 비밀번호 암호화
                request.getPhoneNumber(),
                socialId
        );

        // 현재 구조에서는 이메일 인증 완료 상태로 처리 (추후 변경 가능)
        member.setCertifiedByEmail(true);

        Member savedMember = memberRepository.save(member);

        // 프로필 생성
        profileRepository.save(Profiles.createMemberProfile(member));

        // 기존 기본 주소 초기화
        addressRepository.resetDefaultAddressForMember(savedMember.getId());

        // 회원가입 요청 시 주소가 존재하면 등록
        AddressesReqeustDto addressDto = request.getAddress();
        if (addressDto != null) {
            Addresses newAddress = Addresses.createAddress(addressDto, member);
            newAddress.setDefaultAddress(true);
            addressRepository.save(newAddress);
        }

        // 이메일 인증 발송
        sendVerificationEmail(member.getEmail());
        return member;
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
    @Transactional
    public String login(LoginRequest loginRequest, HttpServletResponse response) {
        // 이메일로 회원 조회 (없으면 Exception)
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일이 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        // 이메일 인증 여부 체크
        if (!member.isCertifiedByEmail()) {
            throw new UnverifiedEmailException("인증되지 않은 이메일입니다.");
        }

        // JWT 발급
        String accessToken = jwtUtil.createAccessToken("access", member.getId().toString(), member.getMemberRole().name());
        String refreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), member.getMemberRole().name());

        // RefreshToken → Redis 저장
        redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);

        // AccessToken → HttpOnly 쿠키로 저장
        Cookie jwtCookie = new Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true); // HTTPS 환경에서만 전송
        jwtCookie.setPath("/");

        // 만료시간 계산 후 쿠키에 설정
        int maxAge = (int) ((jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000);
        jwtCookie.setMaxAge(maxAge);
        response.addCookie(jwtCookie);

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

        // 기존 비밀번호 일치 확인
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new InvalidCredentialsException("기존 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 확인값 일치
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new InvalidCredentialsException("새 비밀번호가 서로 일치하지 않습니다.");
        }

        // 비밀번호 암호화 후 저장
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        log.info("회원 {} 비밀번호가 성공적으로 변경되었습니다.", member.getEmail());
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

    /**
     * 랜덤 소셜 ID 생성
     * - 일반 회원도 내부적으로 고유한 소셜 ID 생성하여 저장
     */
    private String generateSocialId() {
        return "general-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

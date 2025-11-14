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
     * - 이메일 중복 체크
     * - 비밀번호 암호화
     * - 이메일 인증 상태 초기화
     * - 프로필 및 기본 주소 초기화
     * @param request 회원가입 요청 DTO
     * @return 저장된 Member 엔티티
     */
    @Transactional
    public Member registerMember(JoinMemberRequest request) {
        // 1. 이메일 중복 확인
        validateDuplicateEmail(request.getEmail());

        // 2. 랜덤 토큰 및 소셜ID 생성
        String token = UUID.randomUUID().toString();
        String socialId = generateSocialId();

        // 3. Member 엔티티 생성
        Member member = Member.createGeneralMember(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword1()),
                request.getPhoneNumber(),
                socialId);

        // 4. 이메일 인증 상태 true (임시)
        member.setCertifiedByEmail(true);

        // 5. DB 저장 및 프로필 생성
        Member savedMember = memberRepository.save(member);
        profileRepository.save(Profiles.createMemberProfile(member));

        // 6. 기존 기본 주소 초기화
        addressRepository.resetDefaultAddressForMember(savedMember.getId());

        // 7. 전달받은 주소가 있으면 저장
        AddressesReqeustDto addressDto = request.getAddress();
        if (addressDto != null) {
            Addresses newAddress = Addresses.createAddress(addressDto, member);
            newAddress.setDefaultAddress(true);
            addressRepository.save(newAddress);
        }

        // 8. 이메일 인증 발송
        sendVerificationEmail(member.getEmail());

        return member;
    }

    /**
     * 관리자 회원 가입
     * - 일반 회원과 유사하지만 권한 활성화
     * @param request 관리자 회원 가입 DTO
     * @return 저장된 관리자 Member 엔티티
     */
    @Transactional
    public Member registerAdmin(JoinMemberRequest request) {
        // 1. 이메일 중복 확인
        validateDuplicateEmail(request.getEmail());

        // 2. 랜덤 토큰 및 소셜ID 생성
        String token = UUID.randomUUID().toString();
        String socialId = generateSocialId();

        // 3. 관리자 엔티티 생성
        Member admin = Member.createAdminMember(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                passwordEncoder.encode(request.getPassword1()),
                request.getPhoneNumber(),
                socialId);

        // 4. 관리자 계정 활성화
        admin.activate();

        // 5. DB 저장 및 프로필 생성
        memberRepository.save(admin);
        profileRepository.save(Profiles.createMemberProfile(admin));

        return admin;
    }

    /**
     * 로그인 처리
     * - 이메일/비밀번호 검증
     * - 이메일 인증 여부 확인
     * - Access/Refresh Token 생성 후 쿠키와 Redis 저장
     * @param loginRequest 로그인 요청 DTO
     * @param response HTTP 응답 객체 (쿠키 저장용)
     * @return 로그인 성공 메시지
     */
    @Transactional
    public String login(LoginRequest loginRequest, HttpServletResponse response) {
        // 1. 이메일 존재 여부 확인
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일이 올바르지 않습니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        // 3. 이메일 인증 확인
        if (!member.isCertifiedByEmail()) {
            throw new UnverifiedEmailException("인증되지 않은 이메일입니다.");
        }

        // 4. JWT Access/Refresh Token 생성
        String accessToken = jwtUtil.createAccessToken("access", member.getId().toString(), member.getMemberRole().name());
        String refreshToken = jwtUtil.createRefreshToken("refresh", member.getId().toString(), member.getMemberRole().name());

        // 5. Redis에 Refresh Token 저장
        redisRefreshTokenRepository.save(String.valueOf(member.getId()), refreshToken);

        // 6. AccessToken을 HttpOnly 쿠키로 설정
        Cookie jwtCookie = new Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        int maxAge = (int) ((jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000);
        jwtCookie.setMaxAge(maxAge);
        response.addCookie(jwtCookie);

        return "로그인 성공, 환영합니다.";
    }

    /**
     * 안전한 비밀번호 변경
     * - 기존 비밀번호 확인
     * - 새 비밀번호 일치 확인
     * - 암호화 후 DB 저장
     * @param memberId 회원 ID
     * @param currentPassword 기존 비밀번호 (평문)
     * @param newPassword 새 비밀번호 (평문)
     * @param newPasswordConfirm 새 비밀번호 확인 (평문)
     */
    @Transactional
    public void resetPassword(Long memberId, String currentPassword, String newPassword, String newPasswordConfirm) {
        Member member = findMemberById(memberId);

        // 1. 기존 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new InvalidCredentialsException("기존 비밀번호가 올바르지 않습니다.");
        }

        // 2. 새 비밀번호 일치 여부 확인
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new InvalidCredentialsException("새 비밀번호가 서로 일치하지 않습니다.");
        }

        // 3. 새 비밀번호 암호화 후 저장
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        log.info("회원 {} 비밀번호가 성공적으로 변경되었습니다.", member.getEmail());
    }

    /**
     * 이메일 인증 발송
     * @param email 이메일 주소
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
     * @param email 이메일
     */
    public void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ExistingMemberException(email);
        }
    }

    /**
     * 이메일로 회원 조회
     * @param email 이메일
     * @return Member 엔티티
     */
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException(email));
    }

    /**
     * ID로 회원 조회
     * @param id 회원 ID
     * @return Member 엔티티
     */
    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("해당 ID로 사용자를 찾을 수 없습니다: " + id));
    }

    /**
     * 랜덤 소셜 ID 생성
     * @return 소셜 ID 문자열
     */
    private String generateSocialId() {
        return "general-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

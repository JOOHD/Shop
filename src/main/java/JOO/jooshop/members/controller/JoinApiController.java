package JOO.jooshop.members.controller;

import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import JOO.jooshop.global.mail.entity.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JoinApiController {

    private final MemberService memberService;
    private final EmailMemberService emailMemberService;
    private final MemberRepositoryV1 memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshRepository refreshRepository;
    private final ProfileRepository profileRepository;

    /**
     * Profile 추가, return : id -> email
     * 일반 회원에 대한 회원가입 진행. (default) MemberRole = USER, SocialType = GENERAL,
     * 이메일 인증 로직 추가.
     * @param request email, password, nickname
     * @return email
     */
    @PostMapping("/join") // 회원가입
    public ResponseEntity<?> joinMemberV1(@RequestBody @Valid JoinMemberRequest request) {
        // 표준화된 128-bit의 고유 식별자
        String token = UUID.randomUUID().toString();

        // 닉네임 유효성 체크
        if (request.getNickname() == null || request.getNickname().isEmpty()) {
            return new ResponseEntity<>("닉네임을 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        // 회원 가입 처리
        Member member = createMemberFromRequest(request, token);

        try {
            validateExistedMemberByEmail(member.getEmail());
        } catch (MemberService.ExistingMemberException e) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        }

        // 회원가입 및 프로필 저장
        Member joinMember = memberService.joinMember(member);
        Profiles profile = Profiles.createMemberProfile(joinMember);

        // 이메일 인증 메일 전송
        sendVerificationEmail(joinMember.getEmail(), token);

        JoinMemberResponse response = new JoinMemberResponse(joinMember.getId(), member.getEmail());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/admin/join") // 관리자 회원 가입
    public ResponseEntity<?> joinAdmin(@RequestBody @Valid JoinMemberRequest request) {
        // 표준화된 128-bit 고유 식별자
        String token = UUID.randomUUID().toString();

        // 닉네임 유효성 체크
        if (request.getNickname() == null || request.getNickname().isEmpty()) {
            return new ResponseEntity<>("Nickname caanot be empty", HttpStatus.BAD_REQUEST);
        }

        // ADMIN 생성
        Member member = createAdminFromRequest(request, token);
        member.verifyAdminUser();
        member.activateMember();

        try {
            validateExistedMemberByEmail(member.getEmail());
        } catch (MemberService.ExistingMemberException e) {
            return ResponseEntity.badRequest().build();
        }

        // 관리자 회원가입 및 프로필 저장
        Member newAdminMember = memberRepository.save(member);
        Profiles profile = Profiles.createMemberProfile(newAdminMember);
        profileRepository.save(profile);

        JoinMemberResponse response = new JoinMemberResponse(newAdminMember.getId(), newAdminMember.getEmail());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 이메일 인증 메서드
    private void validateExistedMemberByEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            log.error("이미 등록된 이메일입니다.");
            throw new MemberService.ExistingMemberException();
        }
    }

    // 회원가입 이메일 인증 메일 전송
    private void sendVerificationEmail(String email, String token) {
        try {
            Member member = Member.createEmailMember(email, token);
            emailMemberService.sendEmailVerification(member);
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) throws UserPrincipalNotFoundException, CredentialNotFoundException {
        Member member = memberService.memberLogin(loginRequest);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        if (!member.isCertifyByMail()) {
            return ResponseEntity.badRequest().body("이메일 인증이 되지 않은 회원입니다.");
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body("로그인 성공, 환영합니다. ");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshAuthorization", required = false) String refreshAuthorization,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (refreshAuthorization == null || refreshAuthorization.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 로그아웃된 회원입니다.");
        }

        String refreshToken = refreshAuthorization.substring(7);
        Optional<Refresh> optionalRefresh = refreshRepository.findByRefreshToken(refreshToken);

        if (!optionalRefresh.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token 이 존재하지 않습니다.");
        }

        Refresh refreshEntity = optionalRefresh.get();
        CookieUtil.deleteCookie(response, "refreshAuthorization");

        log.info("로그아웃 성공: 멤버 ID - {}", refreshEntity.getMember().getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body("로그아웃 성공, 멤버 ID - " + refreshEntity.getMember().getId());
    }

    private Member createMemberFromRequest(JoinMemberRequest request, String token) {
        // Generate a UUID for socialId
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String socialId = "general-" + uuid.substring(0, 12);
        return Member.createGeneralMember(
                request.email,
                request.nickname,
                passwordEncoder.encode(request.password),
                token,
                socialId
        );
    }

    private Member createAdminFromRequest(JoinMemberRequest request, String token) {
        // Generate a UUID for socialId
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String socialId = "general-" + uuid.substring(0, 12);
        return Member.createAdminMember(
                request.email,
                request.nickname,
                passwordEncoder.encode(request.password),
                token,
                socialId
        );
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


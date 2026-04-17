package JOO.jooshop.admin.members.service;

import JOO.jooshop.global.exception.customException.EmailAlreadyExistsException;
import JOO.jooshop.global.exception.customException.InvalidNicknameException;
import JOO.jooshop.global.exception.customException.UnverifiedEmailException;
import JOO.jooshop.global.mail.service.EmailMemberService;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.request.JoinMemberRequest;
import JOO.jooshop.members.repository.MemberRepository;
import JOO.jooshop.profiile.entity.Profiles;
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
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailMemberService emailMemberService;

    /**
     * 전체 회원 조회
     */
    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * 단일 회원 조회
     */
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. id=" + memberId));
    }

    /**
     * 관리자 계정 생성
     */
    @Transactional
    public Member registerAdmin(JoinMemberRequest request) {

        // 이메일 중복, 닉네임 공백
        validateAdminJoinRequest(request);

        // 관리자 검증
        if (!emailMemberService.isEmailVerified(request.getEmail())) {
            throw new UnverifiedEmailException("이메일 인증이 필요합니다.");
        }

        // 암호화 비밀번호를 넘김 원본 x
        String encodedPassword = passwordEncoder.encode(request.getPassword1());

        // 중요!! 관리자는 socialId가 필요 없지만 왜?
        // “Member 구조상 socialId 자리가 비면 안 되니까, 관리자 전용 식별 문자열을 하나 넣는 것”
        // admin member 도 Member entity를 같이 사용하기 때문
        String socialId = generateAdminSocialId();

        // 관리자 엔티티 생성 (관리자 객체 -> Member 엔티티에 맞춤)
        Member admin = Member.registerAdmin(
                request.getEmail(),
                request.getUsername(),
                request.getNickname(),
                request.getPhoneNumber(),
                encodedPassword,
                socialId
        );

        // 기본 프로필 생성
        Profiles profile = Profiles.createDefaultProfile();
        // Member <-> Profile 연결 (연관관계)
        admin.attachProfile(profile);

        // Member(부모) <- Profile(자식)
        // 부모를 저장해서 자식도 한 번의 흐름으로 끝내는 구조
        return memberRepository.save(admin);
    }

    @Transactional
    public void activate(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.isActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }
        member.activate();
    }

    @Transactional
    public void deactivate(Long memberId) {
        Member member = findMemberById(memberId);
        if (!member.isActive()) {
            throw new IllegalStateException("이미 비활성화된 계정입니다.");
        }
        member.deactivate();
    }

    @Transactional
    public void ban(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.isBanned()) {
            throw new IllegalStateException("이미 정지된 계정입니다.");
        }
        member.ban();
    }

    @Transactional
    public void unban(Long memberId) {
        Member member = findMemberById(memberId);
        if (!member.isBanned()) {
            throw new IllegalStateException("이미 정지 해제된 계정입니다.");
        }
        member.unban();
    }

    @Transactional
    public void expireAccount(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.isAccountExpired()) {
            throw new IllegalStateException("이미 만료된 계정입니다.");
        }
        member.expireAccount();
    }

    @Transactional
    public void renewAccount(Long memberId) {
        Member member = findMemberById(memberId);
        if (!member.isAccountExpired()) {
            throw new IllegalStateException("이미 만료 해제된 계정입니다.");
        }
        member.restoreAccount();
    }

    @Transactional
    public void expirePassword(Long memberId) {
        Member member = findMemberById(memberId);
        if (member.isPasswordExpired()) {
            throw new IllegalStateException("이미 만료된 비밀번호입니다.");
        }
        member.expirePassword();
    }

    @Transactional
    public void renewPassword(Long memberId) {
        Member member = findMemberById(memberId);
        if (!member.isPasswordExpired()) {
            throw new IllegalStateException("이미 만료 해제된 비밀번호입니다.");
        }
        member.restorePassword();
    }

    private void validateAdminJoinRequest(JoinMemberRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("이미 등록된 이메일입니다.");
        }

        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new InvalidNicknameException("닉네임이 올바르지 않습니다.");
        }
    }

    private String generateAdminSocialId() {
        return "admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
package JOO.jooshop.admin.members.service;

import JOO.jooshop.global.exception.customException.EmailAlreadyExistsException;
import JOO.jooshop.global.exception.customException.InvalidNicknameException;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.request.JoinMemberRequest;
import JOO.jooshop.members.repository.MemberRepository;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
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
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 전체 회원 조회
     */
    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * 단일 회원 조회
     */
    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));
    }

    /**
     * 관리자 계정 생성
     */
    @Transactional
    public Member registerAdmin(JoinMemberRequest req) {

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException("이미 등록된 이메일입니다.");
        }

        // 닉네임 중복 또는 올바르지 않은 경우
        if (req.getNickname() == null || req.getNickname().trim().isEmpty()) {
            throw new InvalidNicknameException("닉네임이 올바르지 않습니다.");
        }

        String socialId = generateSocialId();

        Member admin = Member.registerAdmin(
                req.getEmail(),
                req.getUsername(),
                req.getNickname(),
                req.getPhoneNumber(),
                passwordEncoder.encode(req.getPassword1()),
                socialId
        );

        Profiles profile = Profiles.createDefaultProfile();
        admin.attachProfile(profile);

        return memberRepository.save(admin);
    }

    /**
     * 회원 상태 변경
     */
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
            throw new IllegalStateException("이미 비활성화된 계쩡입니다.");
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

    private String generateSocialId() {
        return "admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

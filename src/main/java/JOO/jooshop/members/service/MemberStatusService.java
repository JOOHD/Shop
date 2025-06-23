package JOO.jooshop.members.service;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberStatusService {

    private final MemberRepositoryV1 memberRepository;

    // 회원 정지
    @Transactional
    public void banMember(Long memberId) {
        Member member = getMember(memberId);
        if (member.isBanned()) {
            throw new IllegalStateException("이미 정지된 계정입니다.");
        }
        member.ban(); // 도메인 메서드로 처리
    }

    // 회원 정지 해제
    @Transactional
    public void unbanMember(Long memberId) {
        Member member = getMember(memberId);
        if (!member.isBanned()) {
            throw new IllegalStateException("이미 정지 해제된 계정입니다.");
        }
        member.unban(); // 도메인 메서드로 처리
    }

    // 계정 만료 처리
    @Transactional
    public void accountExpired(Long memberId) {
        Member member = getMember(memberId);
        if (member.accountExpired()) {
            throw new IllegalStateException("이미 만료된 계정입니다.");
        }
        member.accountExpired(); // 도메인 메서드로 처리
    }

    // 내부 멤버 조회
    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
    }
}

package JOO.jooshop.members.service;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberStatusService {

    private final MemberRepositoryV1 memberRepository;

    public void banMember(Long memberId) {
        Member member = getMember(memberId);
        if (member.isBanned()) throw new IllegalStateException("이미 정지된 계정입니다.");
        member.setBanned(true);
        memberRepository.save(member);
    }

    public void unbanMember(Long memberId) {
        Member member = getMember(memberId);
        if (!member.isBanned()) throw new IllegalStateException("이미 정지 해제된 계정입니다.");
        member.setBanned(false);
        memberRepository.save(member);
    }

    public void expireAccount(Long memberId) {
        Member member = getMember(memberId);
        member.setAccountExpired(true);
        memberRepository.save(member);
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
    }
}

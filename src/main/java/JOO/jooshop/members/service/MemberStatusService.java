package JOO.jooshop.members.service;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberStatusService {

    /**
     * | 기능           | 엔드포인트                | 도메인 메서드        | 설명                     |
     * | 계정 활성화     | `/activate/{id}`        | `activate()`       | `active = true`         |
     * | 계정 비활성화    | `/deactivate/{id}`      | `deactivate()`    | `active = false`         |
     * | 계정 정지       | `/ban/{id}`             | `ban()`           | `banned = true`          |
     * | 계정 정지 해제   | `/unban/{id}`           | `unban()`         | `banned = false`         |
     * | 계정 만료       | `/expire-account/{id}`  | `expireAccount()` | `accountExpired = true`  |
     * | 계정 만료 해제   | `/renew-account/{id}`   | `renewAccount()`  | `accountExpired = false` |
     * | 비밀번호 만료    | `/expire-password/{id}` | `expirePassword()`| `passwordExpired = true` |
     * | 비밀번호 만료 해제 | `/renew-password/{id}` | `renewPassword()` | `passwordExpired = false`|
     * | 비밀번호 변경    | `/repassword/{id}`      | `changePassword()`| 비밀번호 변경 후 저장        |
     */

    private final MemberRepositoryV1 memberRepository;

    @Transactional
    public void activate(Long id) {
        Member member = getMember(id);
        if (member.isActive()) throw new IllegalStateException("이미 활성화된 계정입니다.");
        member.activate();
    }

    @Transactional
    public void deactivate(Long id) {
        Member member = getMember(id);
        if (!member.isActive()) throw new IllegalStateException("이미 비활성화된 계정입니다.");
        member.deactivate();
    }

    @Transactional
    public void ban(Long id) {
        Member member = getMember(id);
        if (member.isBanned()) throw new IllegalStateException("이미 정지된 계정입니다.");
        member.ban();
    }

    @Transactional
    public void unban(Long id) {
        Member member = getMember(id);
        if (!member.isBanned()) throw new IllegalStateException("이미 정지 해제된 계정입니다.");
        member.unban();
    }

    @Transactional
    public void expireAccount(Long id) {
        Member member = getMember(id);
        if (member.isAccountExpired()) throw new IllegalStateException("이미 만료된 계정입니다.");
        member.expireAccount();
    }

    @Transactional
    public void renewAccount(Long id) {
        Member member = getMember(id);
        if (!member.isAccountExpired()) throw new IllegalStateException("이미 만료 해제된 계정입니다.");
        member.renewAccount();
    }

    @Transactional
    public void expirePassword(Long id) {
        Member member = getMember(id);
        if (member.isPasswordExpired()) throw new IllegalStateException("이미 만료된 비밀번호입니다.");
        member.expirePassword();
    }

    @Transactional
    public void renewPassword(Long id) {
        Member member = getMember(id);
        if (!member.isPasswordExpired()) throw new IllegalStateException("이미 만료 해제된 비밀번호입니다.");
        member.renewPassword();
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        Member member = getMember(id);
        member.changePassword(newPassword);
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
    }
}
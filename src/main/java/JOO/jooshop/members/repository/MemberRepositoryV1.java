package JOO.jooshop.members.repository;

import JOO.jooshop.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepositoryV1 extends JpaRepository<Member, Long> {

    Boolean existsByUsername(String username);

    // username 을 받아 DB 테이블에서 회원을 조회하는 메소드 작성
    Optional<Member> findByUsername(String username);

    // 식별자를 Email 에서 Social Id 로 변경함으로써, email 은 2개이상의 계정
    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);

    Optional<Member> findByEmail(String email);

    List<Member> findAllByEmail(String email);

    Optional<Member> findBySocialId(String socialId);

    Optional<Member> findByToken(String token);
}

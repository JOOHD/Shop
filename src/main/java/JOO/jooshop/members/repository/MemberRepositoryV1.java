package JOO.jooshop.members.repository;

import JOO.jooshop.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepositoryV1 extends JpaRepository<Member, Long> {

    // username을 받아 해당 username이 이미 존재하는지 확인하는 메소드
    Boolean existsByUsername(String username);

    // username을 받아 DB에서 해당 username을 가진 회원을 조회하는 메소드
    Optional<Member> findByUsername(String username);

    // email을 통해 회원 정보를 조회할 수 있지만,
    // socialId를 사용하여 동일한 이메일로 여러 계정을 가질 수 있기에 이를 대체한 메소드
    Boolean existsByEmail(String email);

    // nickname이 이미 존재하는지 확인하는 메소드
    Boolean existsByNickname(String nickname);

    // email을 통해 회원 정보를 조회하는 메소드
    Optional<Member> findByEmail(String email);

    // 동일한 email을 가진 모든 회원을 조회하는 메소드
    List<Member> findAllByEmail(String email);

    // socialId를 통해 회원 정보를 조회하는 메소드
    Optional<Member> findBySocialId(String socialId);

    // token을 통해 회원 정보를 조회하는 메소드
    Optional<Member> findByToken(String token);
}

package JOO.jooshop.members.repository;

import JOO.jooshop.global.mail.entity.CertificationEntity;
import JOO.jooshop.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepositoryV1 extends JpaRepository<Member, Long> {

    // email을 통해 회원 정보를 조회할 수 있지만,
    // socialId를 사용하여 동일한 이메일로 여러 계정을 가질 수 있기에 이를 대체한 메소드
    Boolean existsByEmail(String email);

    // email을 통해 회원 정보를 조회하는 메소드
    Optional<Member> findByEmail(String email);

    void deleteByEmail(String email);

    // socialId를 통해 회원 정보를 조회하는 메소드
    Optional<Member> findBySocialId(String socialId);

}

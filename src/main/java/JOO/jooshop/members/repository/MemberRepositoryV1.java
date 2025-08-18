package JOO.jooshop.members.repository;

import JOO.jooshop.global.mail.entity.CertificationEntity;
import JOO.jooshop.members.entity.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    // @Modifying : 기본 @Query 는 select 만 실행 가능 → update/delete 는 @Modifying 필요
    // clearAutomatically = true : 이전 메모리에 있던 entity, db 상태가 달라질 때 동기화 문제 방지
    // flushAutomatically = true : db에 아직 반영되지 않은 변경 사항이 있으면 먼저 commit 후, update 쿼리 실행
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE member SET joined_at = CURRENT_TIMESTAMP WHERE joined_at IS NULL", nativeQuery = true)
    int fillNullJoinedAt(); // profile 생성 시, joinedAt = null 방지
}

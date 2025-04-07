package JOO.jooshop.members.repository;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.Refresh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshRepository extends JpaRepository<Refresh, Long> {

    // refresh 존재 여부
    Boolean existsByRefreshToken(String refresh);

    // refresh 토큰 삭제
    @Transactional
    void deleteByRefreshToken(String refresh);

    Optional<Refresh> findByRefreshToken(String refresh);

    Optional<Refresh> findByMember(Member member);
}

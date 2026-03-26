package JOO.jooshop.members.repository;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // refresh 존재 여부
    Boolean existsByRefreshToken(String refresh);

    // refresh 토큰 삭제
    @Transactional
    void deleteByRefreshToken(String refresh);

    Optional<RefreshToken> findByRefreshToken(String refresh);

    Optional<RefreshToken> findByMember(Member member);
}

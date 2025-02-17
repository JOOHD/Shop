package JOO.jooshop.members.repository;

import JOO.jooshop.members.entity.Refresh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshRepository extends JpaRepository<Refresh, Long> {

    Boolean existsByRefreshToken(String refresh);

    @Transactional
    void deleteByRefreshToken(String refresh);

    Optional<Refresh> findByRefreshToken(String refresh);

    Optional<Refresh> findByMemberId(Long id);
}

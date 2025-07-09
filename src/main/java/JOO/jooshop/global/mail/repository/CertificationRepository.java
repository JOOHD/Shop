package JOO.jooshop.global.mail.repository;

import JOO.jooshop.global.mail.entity.CertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificationRepository extends JpaRepository<CertificationEntity, Long> {
    Optional<CertificationEntity> findByToken(String token);
    void deleteByEmail(String email);
}

package JOO.jooshop.payment.repository;

import JOO.jooshop.payment.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByMemberId(Long memberId);
    List<PaymentHistory> findByImpUid(String ImpUid);

}

package JOO.jooshop.payment.repository;

import JOO.jooshop.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    List<PaymentRefund> findByImpUid(String ImpUid);
}

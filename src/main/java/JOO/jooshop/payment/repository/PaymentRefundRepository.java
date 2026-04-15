package JOO.jooshop.payment.repository;

import JOO.jooshop.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * [PaymentRefundRepository]
 * 기존 -> 단순 저장소
 * 리팩토링 -> PaymentRefund aggregate 저장 전용 repository 로 유지
 */
@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
}
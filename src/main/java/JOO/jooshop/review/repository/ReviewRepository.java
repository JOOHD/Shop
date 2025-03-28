package JOO.jooshop.review.repository;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPaymentHistoryProduct(Product product);
    List<Review> findByPaymentHistoryMember(Member member);
    Optional<Review> findByPaymentHistory(PaymentHistory paymentHistory);
}

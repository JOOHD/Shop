package JOO.jooshop.payment.model;

import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.payment.entity.PaymentRefund;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCancelDto {
    private Long paymentHistoryId;
    private PayMethod payMethod;
    private String reason;
    private String refundHolder; // 환불계좌 예금주 : 가상계좌 환불일 경우
    private String refundBank; // 환불계좌 은행코드 : 가상계좌 환불일 경우
    private String refundAccount; // 환불계좌 계좌번호 : 가상계좌 환불일 경우

    public PaymentCancelDto(PaymentRefund paymentRefund) {
        this(
                paymentRefund.getPaymentHistory().getId(),
                paymentRefund.getPaymentHistory().getOrders().getPayMethod(),
                paymentRefund.getReason(),
                paymentRefund.getRefundHolder(),
                paymentRefund.getRefundBank(),
                paymentRefund.getRefundAccount()
        );
    }
}

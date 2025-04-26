package JOO.jooshop.payment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Table(name = "payment_refund")
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_refund_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_history")
    private PaymentHistory paymentHistory;

    @Column(name = "imp_uid")
    private String impUid;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "refund_tel")
    private String refundTel;

    @Column(name = "checksum")
    private Integer checksum;

    @Column(name = "reason")
    private String reason;

    @Column(name = "refund_holder")
    private String refundHolder; // 환불계좌 예금주 : 가상계좌 환불일 경우

    @Column(name = "refund_bank")
    private String refundBank; // 환불계좌 은행코드 : 가상계좌 환불일 경우

    @Column(name = "refund_account")
    private String refundAccount; // 환불계좌 계좌번호 : 가상계좌 환불일 경우

    @Column(name = "refund_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime refundAt;


    public PaymentRefund(PaymentHistory paymentHistory, String impUid, Integer amount, String refundTel, Integer checksum, String reason, String refundHolder, String refundBank, String refundAccount, LocalDateTime refundAt) {
        this.paymentHistory = paymentHistory;
        this.impUid = impUid;
        this.amount = amount;
        this.refundTel = refundTel;
        this.checksum = checksum;
        this.reason = reason;
        this.refundHolder = refundHolder;
        this.refundBank = refundBank;
        this.refundAccount = refundAccount;
        this.refundAt = refundAt;
    }

    public PaymentRefund(PaymentHistory paymentHistory, String impUid, Integer amount, String refundTel, Integer checksum, String reason, String refundHolder, String refundBank, String refundAccount) {
        this.paymentHistory = paymentHistory;
        this.impUid = impUid;
        this.amount = amount;
        this.refundTel = refundTel;
        this.checksum = checksum;
        this.reason = reason;
        this.refundHolder = refundHolder;
        this.refundBank = refundBank;
        this.refundAccount = refundAccount;
        this.refundAt =  LocalDateTime.now();
    }

    public PaymentRefund(PaymentHistory paymentHistory, String impUid, Integer amount, String refundTel, Integer checksum, String reason) {
        this.paymentHistory = paymentHistory;
        this.impUid = impUid;
        this.amount = amount;
        this.refundTel = refundTel;
        this.checksum = checksum;
        this.reason = reason;
        this.refundAt =  LocalDateTime.now();
    }

    public PaymentRefund(String impUid, Integer refundAmount, Integer checksum) {
        this.impUid = impUid;
        this.amount = refundAmount;
        this.checksum = checksum;
    }
}

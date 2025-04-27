package JOO.jooshop.payment.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_history_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "orders", nullable = false)
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "product", nullable = false)
    private Product product;

    @Column(name = "impUid")
    private String impUid;

    @Column(name = "pay_method")
    private String payMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.COMPLETE; // 기본값 설정도 가능

    private String bankCode;
    private String bankName;
    private String buyerAddr;
    private String buyerEmail;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_option")
    private String productOption;

    @Column(name = "product_price", nullable = false)
    private BigDecimal price;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "paid_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status statusType;

    @Column(name = "review")
    @Builder.Default
    private Boolean review = false;

    @Column(name = "quantity")
    private Long quantity;

    // 빌더에서 호출할 생성자 수정
    public PaymentHistory(String impUid, Member member, Orders orders, Product product,
                          String productName, String productOption, Long quantity,
                          BigDecimal price, BigDecimal totalPrice, Status statusType,
                          String payMethod, String bankCode, String bankName,
                          String buyerAddr, String buyerEmail) {
        this.impUid = impUid;
        this.member = member;
        this.orders = orders;
        this.product = product;
        this.productName = productName;
        this.productOption = productOption;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
        this.statusType = statusType;
        this.payMethod = payMethod;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.buyerAddr = buyerAddr;
        this.buyerEmail = buyerEmail;

    }

    public void setReview(Boolean review) {
        this.review = review;
    }

    public void updateStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }


    // 첫 번째 썸네일 경로를 가져오는 헬퍼 메서드 추가
    // 썸네일 > 상품 이미지 (성능, UX 이유)
    public String getFirstThumbnailImagePath() {
        if (product != null && !product.getProductThumbnails().isEmpty()) {
            return product.getProductThumbnails().get(0).getImagePath();
        }
        return null;
    }
}

package JOO.jooshop.payment.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

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

    private String bankCode;
    private String bankName;
    private String buyerAddr;
    private String buyerEmail;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_option")
    private String productOption;

    @Column(name = "product_price", nullable = false)
    private Integer price;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

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
                          Integer price, Integer totalPrice, Status statusType,
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

    public void setStatusType(Status statusType) {
        this.statusType = statusType;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
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

package JOO.jooshop.order.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentStatus;
import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class Orders {

    /**
     Aggregate Root
     역할:
         주문 전체 대표
         주문 상태 관리
         주문 자식(OrderProduct) 생명주기 관리
         결제 전/후 상태 변경 관리
         외부 서비스가 주문 내부를 직접 조립하지 못하게 막는 중심
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @Column(name = "order_name", nullable = false)
    private String ordererName;

    @Column(name = "product_name", nullable = false)
    private String productNameSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_method", nullable = false)
    private PayMethod payMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 100, name = "merchant_uid", nullable = false, unique = true)
    private String merchantUid;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "post_code", length = 100, nullable = false)
    private String postCode;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime orderDay;

    @OneToMany(mappedBy = "orders")
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    private Orders(
            Member member,
            String ordererName,
            String phoneNumber,
            String postCode,
            String address,
            String detailAddress,
            PayMethod payMethod,
            String merchantUid
    ) {
        validateCreate(member, ordererName, phoneNumber, postCode, address, payMethod, merchantUid);

        this.member = member;
        this.ordererName = ordererName;
        this.phoneNumber = phoneNumber;
        this.postCode = postCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.payMethod = payMethod;
        this.merchantUid = merchantUid;
        this.paymentStatus = PaymentStatus.COMPLETE; // 현재 프로젝트 흐름 유지, 추후 READY로 변경 가능
        this.totalPrice = BigDecimal.ZERO;
        this.productNameSummary = "";
    }

    public static Orders createOrder(
            Member member,
            String ordererName,
            String phoneNumber,
            String postCode,
            String address,
            String detailAddress,
            PayMethod payMethod,
            String merchantUid
    ) {
        return new Orders(
                member,
                ordererName,
                phoneNumber,
                postCode,
                address,
                detailAddress,
                payMethod,
                merchantUid
        );
    }

    public void addOrderProducts(OrderProduct orderProduct) {
        if (orderProduct == null) {
            throw new IllegalArgumentException("주문 상품은 null일 수 없습니다.");
        }

        this.orderProducts.add(orderProduct);
        orderProduct.attachTo(this);
        recalculateOrderSummary();
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        if (orderProduct == null) {
            throw new IllegalArgumentException("주문 상품은 null일 수 없습니다.");
        }

        this.orderProducts.add(orderProduct);
        orderProduct.attachTo(this);
        recalculateOrderSummary();
    }

    public void addOrderProducts(List<OrderProduct> orderProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 최소 1개 이상이어야 합니다.");
        }

        orderProducts.forEach(this::addOrderProducts);
    }

    public void changePaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new IllegalArgumentException("결제 상태는 null일 수 없습니다.");
        }
        this.paymentStatus = paymentStatus;
    }

    public void markPaid() {
        this.paymentStatus = PaymentStatus.COMPLETE;
    }

    public void markCanceled() {
        this.paymentStatus = PaymentStatus.CANCELED;
    }

    private void recalculateOrderSummary() {
        this.totalPrice = this.orderProducts.stream()
                .map(OrderProduct::calculateLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.productNameSummary = this.orderProducts.stream()
                .map(OrderProduct::getProductName)
                .reduce((first, second) -> first + "," + second)
                .orElse("");
    }

    private void validateCreate(
            Member member,
            String ordererName,
            String phoneNumber,
            String postCode,
            String address,
            PayMethod payMethod,
            String merchantUid
    ) {
        if (member == null) throw new IllegalArgumentException("주문 회원은 필수입니다.");
        if (isBlank(ordererName)) throw new IllegalArgumentException("주문자명은 필수입니다.");
        if (isBlank(phoneNumber)) throw new IllegalArgumentException("전화번호는 필수입니다.");
        if (isBlank(postCode)) throw new IllegalArgumentException("우편번호는 필수입니다.");
        if (isBlank(address)) throw new IllegalArgumentException("주소는 필수입니다.");
        if (payMethod == null) throw new IllegalArgumentException("결제수단은 필수입니다.");
        if (isBlank(merchantUid)) throw new IllegalArgumentException("merchantUid는 필수입니다.");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

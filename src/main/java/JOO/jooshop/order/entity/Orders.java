package JOO.jooshop.order.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentStatus;
import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@Table(name = "orders")
public class Orders {

    /*
        Product (PK)
        ProductMgt (FK, Product 참조, 연결점)
        Orders (JoinColumns)
        Orders_ProductManagement (InverseJoinColumns, 양방향 연결점)

        Member (1) <-> Orders (N) <-> ProductManagement (N) <-> Product (1)
        - 회원(1명)은 여러 주문(N개) 가능
        - 하나의 주문에는 여러 개의 상품 옵션이 있을 수 있고,
            여러 주문이 동일한 상품 옵션을 가질 수 있다. (다대다)
        - 상품 관리 항목은 하나의 상품을 참조 (1:N)
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToMany // 다대다 연결을 표현하기 위해 중간 테이블이 자동 생성된다.
    @JoinTable(
            name = "orders_product_management", // JoinTable : 현재 어떤 엔티티와 연결이 되었는지 명시적 설정, DB에 정확하게 저장 위해
            joinColumns = @JoinColumn(name = "orders_id"),  // JoinColumn : FK 를 사용하기 위해
            inverseJoinColumns = @JoinColumn(name = "product_management_id") // InverseJoinColumn : 상대 엔티티의 외래 키 설정
    )
    private List<ProductManagement> productManagements = new ArrayList<>();

    @Column(name = "order_name", nullable = false)
    private String ordererName;

    // 문자열로 저장, 쉼표로 구분된 형태로 저장
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    PayMethod payMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.COMPLETE; // 기본값 설정도 가능

    @Column(length = 100, name = "merchant_uid")
    private String merchantUid;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "post_code", length = 100, nullable = false)
    private String postCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime orderDay;


    @OneToMany(mappedBy = "orders")
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    public Orders() {
        this.orderDay = LocalDateTime.now();
    }

    public Orders(Member member,
                  List<ProductManagement> productManagements,
                  String ordererName,
                  String productName,
                  BigDecimal totalPrice,
                  String phoneNumber,
                  String postCode,
                  String address,
                  String detailAddress,
                  PayMethod payMethod,
                  String merchantUid) {
        this.member = member;
        this.productManagements = productManagements;
        this.ordererName = ordererName;
        this.productName = productName;
        this.totalPrice = totalPrice;
        this.phoneNumber = phoneNumber;
        this.postCode = postCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.payMethod = payMethod;
        this.merchantUid = merchantUid;
    }

    public void updatePayStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

}

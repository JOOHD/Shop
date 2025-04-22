package JOO.jooshop.order.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@Entity
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

    @Column(name = "product_name", nullable = false)
    private String productNames;

    @Enumerated(EnumType.STRING)
    PayMethod payMethod;

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

    @Column(name = "payment_status")
    private Boolean paymentStatus = false;

    @OneToMany(mappedBy = "orders")
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    public Orders() {
        this.orderDay = LocalDateTime.now();
    }

    public Orders(Member member, List<ProductManagement> productManagements, String ordererName, String productNames, BigDecimal totalPrice, String phoneNumber) {
        this.member = member;
        this.productManagements = productManagements;
        this.ordererName = ordererName;
        this.productNames = productNames;
        this.totalPrice = totalPrice;
        this.phoneNumber = phoneNumber;
    }


    public void orderConfirm(String merchantUid, OrderDto orderDto) {
        this.merchantUid = merchantUid;
        this.postCode = orderDto.getPostCode();
        this.address = orderDto.getAddress();
        this.detailAddress = orderDto.getDetailAddress();
        this.ordererName = orderDto.getOrdererName();
        this.phoneNumber = orderDto.getPhoneNumber();
        this.payMethod = orderDto.getPayMethod();
        this.orderDay = LocalDateTime.now();

    }

    /*
    public void setMember(Member member) {
        this.member = member;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    */

    public void setProductName(List<String> productNameList) {
        if (productNameList != null && !productNameList.isEmpty()) {
            this.productNames = String.join(",", productNameList);
        } else {
            this.productNames = ""; //
        }
    }

    public List<String> getProductNames() {
        return Arrays.asList(this.productNames.split(",")); // String을 다시 List<String>으로 변환
    }

    public void setPaymentStatus(Boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }


}

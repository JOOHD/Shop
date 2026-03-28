package JOO.jooshop.order.entity;

import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"orders", "productManagement"})
@Table(name = "order_product")
public class OrderProduct {

    /**
     Orders의 자식 엔티티
     역할:
         주문 당시 상품 스냅샷 보존
         상품명 / 옵션 / 수량 / 가격 / 썸네일 등 기록
         자신이 어느 주문에 속하는지 관리
         주문 단위에 묶인 상품 행(row) 역할

     중요:
         Product와는 “현재 상품 정보”
         OrderProduct는 “주문 당시 확정 정보”

     즉, 주문 후 상품명이 바뀌어도 주문 기록은 안 바뀌어야 하니까 스냅샷 개념이 필요함.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_management_id", nullable = false)
    private ProductManagement productManagement;

    @Column(name = "price_at_order", nullable = false)
    private BigDecimal priceAtOrder;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_size")
    private String productSize;

    @Column(name = "product_img")
    private String productImg;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reviewed", nullable = false)
    private boolean reviewed;

    @Column(name = "returned", nullable = false)
    private boolean returned;

    private OrderProduct(
            ProductManagement productManagement,
            String productName,
            String productSize,
            String productImg,
            BigDecimal priceAtOrder,
            int quantity
    ) {
        if (productManagement == null) throw new IllegalArgumentException("상품 옵션은 필수입니다.");
        if (priceAtOrder == null || priceAtOrder.signum() < 0) {
            throw new IllegalArgumentException("주문 당시 가격은 0 이상이어야 합니다.");
        }
        if (quantity <= 0) throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");

        this.productManagement = productManagement;
        this.productName = productName;
        this.productSize = productSize;
        this.productImg = productImg;
        this.priceAtOrder = priceAtOrder;
        this.quantity = quantity;
        this.reviewed = false;
        this.returned = false;
    }

    public static OrderProduct createOrderProduct(
            ProductManagement productManagement,
            String productName,
            String productSize,
            String productImg,
            BigDecimal priceAtOrder,
            int quantity
    ) {
        return new OrderProduct(
                productManagement,
                productName,
                productSize,
                productImg,
                priceAtOrder,
                quantity
        );
    }

    void attachTo(Orders orders) {
        this.orders = orders;
    }

    public void completeReview() {
        this.reviewed = true;
    }

    public void markReturned() {
        this.returned = true;
    }

    public BigDecimal calculateLineTotal() {
        return this.priceAtOrder.multiply(BigDecimal.valueOf(this.quantity));
    }
}
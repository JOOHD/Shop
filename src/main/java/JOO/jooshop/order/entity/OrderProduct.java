package JOO.jooshop.order.entity;

import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(exclude = {"orders", "productManagement"}) // 양방향 관계 시 무한 루프 방지
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id")
    private Orders orders;

    // 상품 옵션과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_management_id")
    private ProductManagement productManagement;

    // 주문 당시 가격
    private BigDecimal priceAtOrder;

    // 상품명
    private String productName;

    // 상품 사이즈
    private String productSize;

    // 상품 이미지 URL (썸네일)
    private String productImg;

    // 주문 수량
    private int quantity;

    // 리뷰 작성 여부
    private boolean reviewed;

    // 반품 여부
    private boolean returned;

    /**
     * static factory: 주문 확정 시 사용
     * @param orders Orders 엔티티 (연결 전엔 null 가능)
     * @param productManagement ProductManagement 엔티티
     * @param productName 상품명
     * @param productSize 사이즈
     * @param productImg 이미지 URL
     * @param priceAtOrder 주문 당시 가격
     * @param quantity 수량
     * @return OrderProduct 인스턴스
     */
    public static OrderProduct createOrderProduct(Orders orders,
                                                  ProductManagement productManagement,
                                                  String productName,
                                                  String productSize,
                                                  String productImg,
                                                  BigDecimal priceAtOrder,
                                                  int quantity) {
        OrderProduct op = new OrderProduct();
        op.orders = orders;
        op.productManagement = productManagement;
        op.productName = productName;
        op.productSize = productSize;
        op.productImg = productImg;
        op.priceAtOrder = priceAtOrder;
        op.quantity = quantity;
        op.reviewed = false;
        op.returned = false;
        return op;
    }

    /**
     * Orders 엔티티와 연결
     */
    public void setOrders(Orders orders) {
        this.orders = orders;
    }

    /** 리뷰 완료 처리 */
    public void completeReview() {
        this.reviewed = true;
    }

    /** 반품 처리 */
    public void returnProduct() {
        this.returned = true;
    }
}

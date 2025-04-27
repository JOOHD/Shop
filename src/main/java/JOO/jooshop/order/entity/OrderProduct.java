package JOO.jooshop.order.entity;

import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderProduct {

    /**
     * 장바구니 (Cart) vs 주문 (OrderProduct)
     * Cart : 결제가 이루어지지 않은 일시적인 상태
     * OrderProduct : 사용자가 주문을 최종 확정할 때,
     *      장바구니에 담긴 상품을 바탕으로 주문된 상품 정보가 OrderProduct 로 변환되어야 한다.
     *
     * productName과 priceAtOrder는 Product에서 이미 관리되고 있는 정보들이지만,
     * 주문 당시의 가격과 상품명은 주문 내역에서 OrderProduct에 필요하므로 중복되는 부분은 아닙니다.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id")
    private Orders orders;

    // 상품과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_management_id")
    private ProductManagement productManagement;

    // 주문 당시 가격 (상품 가격 변동 대비)
    private BigDecimal priceAtOrder;

    // 상품명 (OrderProduct에서 직접 관리)
    private String productName;
    
    // 배송지 주소

    // 주문 수량
    private Long quantity;

    // 리뷰 작성 여부
    private boolean reviewed;

    // 반품 여부
    private boolean returned;

    // 생성 메서드
    public static OrderProduct createOrderProduct(Orders orders,
                                                  ProductManagement productManagement,
                                                  BigDecimal priceAtOrder,
                                                  Long quantity) {
        return OrderProduct.builder()
                .orders(orders)
                .productManagement(productManagement)
                .priceAtOrder(priceAtOrder)
                .productName(productManagement.getProduct().getProductName()) // 상품명 추가
                .quantity(quantity)
                .reviewed(false)
                .returned(false)
                .build();
    }

    // 리뷰 완료 메서드
    public void completeReview() {
        this.reviewed = true;
    }

    // 반품 처리 메서드
    public void returnProduct() {
        this.returned = true;
    }

}

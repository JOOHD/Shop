package JOO.jooshop.order.entity;

import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data // 양방향 관계에서는 (toString() / equals() 인한 스택오버플로우 무한 버프 위험
@ToString // 스택오버플로우 무한 루프 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderProduct {

    /**
     * Cart -> OrderProduct 변환
     * 임시 주문 -> 실제 주문 확정 시, 상품 정보
     */

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
     * 생성 메서드: 주문 확정 시 사용
     */
    public static OrderProduct createOrderProduct(Orders orders,
                                                  ProductManagement productManagement,
                                                  BigDecimal priceAtOrder,
                                                  int quantity) {
        var product = productManagement.getProduct();

        return OrderProduct.builder()
                .orders(orders)
                .productManagement(productManagement)
                .priceAtOrder(priceAtOrder)
                .productName(product.getProductName())
                .productSize(productManagement.getSize() != null ? productManagement.getSize().name() : null)
                .productImg(product.getProductThumbnails().isEmpty() ? null
                        : product.getProductThumbnails().get(0).getImagePath())
                .quantity(quantity)
                .reviewed(false)
                .returned(false)
                .build();
    }

    /**
     * 리뷰 완료 처리
     */
    public void completeReview() {
        this.reviewed = true;
    }

    /**
     * 반품 처리
     */
    public void returnProduct() {
        this.returned = true;
    }

}

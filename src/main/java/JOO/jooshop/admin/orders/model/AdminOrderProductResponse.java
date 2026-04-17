package JOO.jooshop.admin.orders.model;

import JOO.jooshop.order.entity.OrderProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderProductResponse {

    private String productName;
    private String productSize;
    private String productImg;
    private int quantity;
    private BigDecimal priceAtOrder;

    /**
     * OrderProduct 엔티티 → 관리자 주문 상품 응답 DTO
     */
    public static AdminOrderProductResponse from(OrderProduct product) {
        return AdminOrderProductResponse.builder()
                .productName(product.getProductName())
                .productSize(product.getProductSize())
                .productImg(product.getProductImg())
                .quantity(product.getQuantity())
                .priceAtOrder(product.getPriceAtOrder())
                .build();
    }
}
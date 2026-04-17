package JOO.jooshop.admin.orders.model;

import JOO.jooshop.order.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderListResponse {

    /**
     * 주문번호
     * 주문자명
     * 전화번호
     * 총액
     * 상태
     * 주문일
     * 상품 요약
     */

    private Long orderId;
    private String ordererName;
    private String phoneNumber;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime orderDate;
    private String productSummary;

    /**
     * Orders 엔티티 → 관리자 주문 목록 응답 DTO
     */
    public static AdminOrderListResponse from(Orders order) {
        return AdminOrderListResponse.builder()
                .orderId(order.getOrderId())
                .ordererName(order.getOrdererName())
                .phoneNumber(order.getPhoneNumber())
                .totalPrice(order.getTotalPrice())
                .status(order.getPaymentStatus().name())
                .orderDate(order.getOrderDay())
                .productSummary(order.getProductNameSummary())
                .build();
    }
}
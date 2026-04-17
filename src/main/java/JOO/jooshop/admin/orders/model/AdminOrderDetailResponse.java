package JOO.jooshop.admin.orders.model;

import JOO.jooshop.order.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderDetailResponse {

    /**
     * 주소
     * 상세주소
     * 우편번호
     * 상품 목록 전체
     */

    private Long orderId;
    private String ordererName;
    private String phoneNumber;
    private String address;
    private String detailAddress;
    private String postCode;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime orderDate;
    private String productSummary;
    private List<AdminOrderProductResponse> products;

    /**
     * Orders 엔티티 → 관리자 주문 상세 응답 DTO
     */
    public static AdminOrderDetailResponse from(Orders order) {
        return AdminOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .ordererName(order.getOrdererName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .detailAddress(order.getDetailAddress())
                .postCode(order.getPostCode())
                .totalPrice(order.getTotalPrice())
                .status(order.getPaymentStatus().name())
                .orderDate(order.getOrderDay())
                .productSummary(order.getProductNameSummary())
                .products(order.getOrderProducts().stream()
                        .map(AdminOrderProductResponse::from)
                        .toList())
                .build();
    }
}
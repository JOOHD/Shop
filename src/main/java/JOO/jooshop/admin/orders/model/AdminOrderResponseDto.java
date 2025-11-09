package JOO.jooshop.admin.orders.model;

import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.order.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminOrderResponseDto {

    private Long orderId;
    private String ordererName;
    private String phoneNumber;
    private String address;
    private String detailAddress;
    private String postCode;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime orderDate;
    private List<AdminOrderProductDto> products;

    /* DTO -> Entity */
    public static AdminOrderResponseDto toEntity(Orders order) {
        List<AdminOrderProductDto> productDtos = order.getOrderProducts().stream()
                .map(AdminOrderProductDto::toEntity)
                .collect(Collectors.toList());

        return AdminOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .ordererName(order.getOrdererName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .detailAddress(order.getDetailAddress())
                .postCode(order.getPostCode())
                .totalPrice(order.getTotalPrice())
                .status(order.getPaymentStatus().name())
                .orderDate(order.getOrderDay())
                .products(productDtos)
                .build();
    }
}

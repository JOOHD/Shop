package JOO.jooshop.admin.orders.model;

import JOO.jooshop.order.entity.OrderProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminOrderProductDto {

    private String productName;
    private String productSize;
    private String productImg;
    private int quantity;
    private BigDecimal priceAtOrder;

    public static AdminOrderProductDto from(OrderProduct product) {
        return AdminOrderProductDto.builder()
                .productName(product.getProductName())
                .productSize(product.getProductSize())
                .productImg(product.getProductImg())
                .quantity(product.getQuantity())
                .priceAtOrder(product.getPriceAtOrder())
                .build();
    }
}

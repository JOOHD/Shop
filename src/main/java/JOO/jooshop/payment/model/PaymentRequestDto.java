package JOO.jooshop.payment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class PaymentRequestDto {
    private Long memberId;
    private Long orderId;
    private BigDecimal price;
    private List<Long> inventoryIdList;

    public PaymentRequestDto(Long memberId, Long orderId, BigDecimal price, List<Long> inventoryIdList) {
        this.memberId = memberId;
        this.orderId = orderId;
        this.price = price;
        this.inventoryIdList = inventoryIdList;
    }
}

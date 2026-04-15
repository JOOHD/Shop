package JOO.jooshop.payment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/*
 * [PaymentRequestDto]
 * 기존 -> 단순 요청 DTO
 * 리팩토링 -> 결제 요청에 필요한 식별자와 금액만 전달하는 요청 DTO 로 유지
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private Long memberId;
    private Long orderId;
    private BigDecimal price;
    private List<Long> inventoryIdList;
}
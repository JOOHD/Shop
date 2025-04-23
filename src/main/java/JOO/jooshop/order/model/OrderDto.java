package JOO.jooshop.order.model;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.enums.PayMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private Long memberId; // 추가 25.04.22
    private List<Long> cartIds;

    @NotNull(message = "우편번호는 필수입니다.")
    private String postCode;

    @NotNull(message = "주소는 필수입니다.")
    private String address;

    private String detailAddress;

    @NotNull(message = "이름은 필수입니다.")
    private String ordererName;

    private String phoneNumber;

    private PayMethod payMethod;

    private String merchantUid;

    private String generateMerchantUid() {
        return "merchant_" + UUID.randomUUID().toString();
    }
}

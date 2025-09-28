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

    /**
     * 클라이언트에서 상품명/사이즈/이미지까지 전송할 필요는 없음.
     * 임시 주문/최종 주문 생성 시 서버에서 Redis 또는 DB에서 조회해서 채움
     */

    private Long memberId;      // 추가 25.04.22
    private List<Long> cartIds; // 추가 25.04.22 -> 수정 25.09.05 hidden

    @NotNull(message = "우편번호는 필수입니다.")
    private String postCode;

    @NotNull(message = "주소는 필수입니다.")
    private String address;

    private String detailAddress;

    @NotNull(message = "이름은 필수입니다.")
    private String username;

    private String ordererName;

    private String phoneNumber;

    private PayMethod payMethod;

    private String merchantUid;
}

package JOO.jooshop.order.model;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.enums.PayMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    @NotNull(message = "우편번호는 필수입니다.")
    private String postCode;
    @NotNull(message = "주소는 필수입니다.")
    private String address;
    private String detailAddress;
    @NotNull(message = "이름은 필수입니다.")
    private String ordererName;
    private String phoneNumber;
    PayMethod payMethod;

    public OrderDto(Orders order) {
        this(
                order.getPostCode(),
                order.getAddress(),
                order.getDetailAddress(),
                order.getOrdererName(),
                order.getPhoneNumber(),
                order.getPayMethod()
        );
    }
}

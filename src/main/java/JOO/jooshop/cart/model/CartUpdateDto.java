package JOO.jooshop.cart.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartUpdateDto {

    /*
        memberId를 DTO에 두면 안 되는 이유는 명확해:
        - 회원 식별은 인증 정보에서 가져와야 함
        - 요청 body는 수정 대상 값만 가져야 함
        - 클라이언트가 소유권을 주장하면 안 됨
     */

    @NotNull
    @Min(1)
    private Integer quantity;
}
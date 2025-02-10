package JOO.jooshop.cart.model;

import JOO.jooshop.cart.entity.Cart;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartUpdateDto {
    @NotNull(message = "수량은 필수로 입력해야 합니다.")
    @Min(value = 1L, message = "수량은 1 이상이어야 합니다.")
    private Long quantity;

    public CartUpdateDto(Cart cart) {
        this(
                cart.getQuantity()
        );
    }
}

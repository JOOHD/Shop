package JOO.jooshop.cart.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {
    private Long memberId;
    private List<CartDto> cartItems;
}

package JOO.jooshop.cart.model;

import lombok.Getter;

import java.util.List;

/**
 * 장바구니 목록 응답 DTO
 */
@Getter
public class CartResponse {

    private final Long memberId;
    private final List<CartDto> cartItems;

    public CartResponse(Long memberId, List<CartDto> cartItems) {
        this.memberId = memberId;
        this.cartItems = cartItems;
    }
}
package JOO.jooshop.cart.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
import JOO.jooshop.cart.model.CartUpdateDto;
import JOO.jooshop.cart.service.CartService;
import JOO.jooshop.global.ResponseMessageConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ModelMapper modelMapper;

    /**
     * 장바구니 담기
     * @param request
     * @param productMgtId
     * @return
     */
    @PostMapping("/add/{productMgtId}")
    public ResponseEntity<String> addCart(@Valid @RequestBody CartRequestDto request, @PathVariable Long productMgtId) {

        Long createdId = cartService.addCart(request, productMgtId);

        return ResponseEntity.ok("장바구니에 등록되었습니다. cart_id : " + createdId);
    }

    /**
     * 내 장바구니 리스트
     * @param memberId
     * @return
     */
    @GetMapping("/{memberId}")
    public List<CartDto> getMyCarts(@PathVariable Long memberId) {
        return cartService.allCarts(memberId);
    }

    /**
     * 장바구니 수정(상품 갯수 수정)
     * @param cartId
     * @param request
     * @return
     */
    @PutMapping("/{cartId}")
    public ResponseEntity<CartDto> updateCart(@PathVariable Long cartId, @Valid @RequestBody CartUpdateDto request) {
        Cart updatedCart = modelMapper.map(request, Cart.class);
        CartDto updatedCartDto = new CartDto(cartService.updateCart(cartId, updatedCart));

        return ResponseEntity.ok(updatedCartDto);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }
}





















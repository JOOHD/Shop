package JOO.jooshop.cart.controller;

import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
import JOO.jooshop.cart.model.CartResponse;
import JOO.jooshop.cart.model.CartUpdateDto;
import JOO.jooshop.cart.service.CartService;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.global.exception.ResponseMessageConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;

    @PostMapping("/add/{inventoryId}")
    public ResponseEntity<String> addCart(@Valid @RequestBody CartRequestDto request,
                                          @PathVariable Long inventoryId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        Long createdId = cartService.addCart(memberId, inventoryId, request.getQuantity());

        return ResponseEntity.ok("장바구니에 추가 되었습니다. cart_id : " + createdId);
    }

    @GetMapping("/my")
    public ResponseEntity<CartResponse> getMyCarts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        List<CartDto> carts = cartService.allCarts(memberId);

        return ResponseEntity.ok(new CartResponse(memberId, carts));
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CartDto> updateCart(@PathVariable Long cartId,
                                              @Valid @RequestBody CartUpdateDto request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        CartDto updatedCartDto = cartService.updateCart(memberId, cartId, request);

        return ResponseEntity.ok(updatedCartDto);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable Long cartId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        cartService.deleteCart(cartId, memberId);

        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }

    @DeleteMapping("/batch-delete")
    public ResponseEntity<String> deleteCartList(@RequestBody List<Long> cartIds,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        cartService.deleteCartList(cartIds, memberId);

        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }
}
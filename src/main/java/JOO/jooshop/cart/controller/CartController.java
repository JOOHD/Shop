package JOO.jooshop.cart.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
import JOO.jooshop.cart.model.CartUpdateDto;
import JOO.jooshop.cart.service.CartService;
import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final MemberRepositoryV1 memberRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;

    /**
     * 장바구니 담기
     * @param request
     * @param productMgtId
     * @return
     */
    @PostMapping("/add/{productMgtId}")
    public ResponseEntity<String> addCart(@Valid @RequestBody CartRequestDto request, @PathVariable("productMgtId") Long productMgtId) {
        Long createdId = cartService.addCart(request, productMgtId);

        return ResponseEntity.ok("장바구니에 등록되었습니다. cart_id : " + createdId);
    }

    /**
     * 내 장바구니 리스트
     * @param memberId
     * @return
     */
    @GetMapping("/{memberId}")
    public List<CartDto> getMyCarts(@PathVariable("memberId") Long memberId) {
        return cartService.allCarts(memberId);
    }

    /**
     * 장바구니 수정(상품 갯수 수정)
     * @param cartId
     * @param request
     * @return
     */
    @PutMapping("/{cartId}")
    public ResponseEntity<CartDto> updateCart(@PathVariable("cartId") Long cartId, @Valid @RequestBody CartUpdateDto request) {

        // 1. Member 직접 주입
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new NoSuchElementException("회원 정보가 없습니다."));

        // 2. DTO -> Entity 매핑 후, Member 주입
        Cart updatedCart = modelMapper.map(request, Cart.class);
        updatedCart.setMember(member);

        // 3. 서비스 호출
        Cart savedCart = cartService.updateCart(cartId, updatedCart, request);

        // 4. 저장된 Cart -> CartDto 로 변환해서 반환
        CartDto updatedCartDto = new CartDto(savedCart);
        return ResponseEntity.ok(updatedCartDto);
    }

    /**
     * 장바구니 삭제
     * @param cartId
     * @return
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable("cartId") Long cartId) {
        cartService.deleteCart(cartId);

        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }
}





















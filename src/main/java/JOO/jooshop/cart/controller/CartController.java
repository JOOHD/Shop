package JOO.jooshop.cart.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
import JOO.jooshop.cart.model.CartUpdateDto;
import JOO.jooshop.cart.service.CartService;
import JOO.jooshop.global.Exception.ResponseMessageConstants;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * 
     * 기존 문제
     * - CartRequestDto 에 memberId 가 포함되어 있고, 클라이언트가 직접 memberId 를 보냄
     * - 클라이언트가 임의로 다른 memberId 를 보낼 수 있어 보안상 위험
     * - 인증 관련 로직이 컨트롤러에 포함되어 있지 않아 인증된 사용자 정보 활용이 어려움
     * 
     * 리팩토링 방향 25.08.11
     * - 클라이언트가 memberId 를 보내지 않도록 변경
     * - JWT 인증을 통해 서버에서 현재 로그인한 memberId 를 가져옴
     * - @AuthenticationPrincipal 을 통해 CustomUserDetails(인증된 사용자 정보)를 받아서 memberId 추출
     * - 요청 바디에는 실제 필요한 데이터(quantity 등)만 받음
     */
    @PostMapping("/add/{productMgtId}") // (POST /api/v1/cart/add...)
    public ResponseEntity<String> addCart(@Valid @RequestBody CartRequestDto request,
                                          @PathVariable("productMgtId") Long productMgtId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long memberId = userDetails.getMemberId(); // JWT 인증 정보에서 직접 추출
        Long createdId = cartService.addCart(memberId, productMgtId, request.getQuantity());

        return ResponseEntity.ok("장바구니에 추가 되었습니다. cart_id : " + createdId);
    }

    /**
     * 내 장바구니 전체 조회
     * @param userDetails 인증된 사용자 정보
     */
    @GetMapping("/my")
    public List<CartDto> getMyCarts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        return cartService.allCarts(memberId);
    }

    /**
     * 장바구니 수량 수정
     * @param cartId 수정할 장바구니 ID
     * @param request 수량 수정 DTO
     * @param userDetails 인증된 사용자 정보
     */
    @PutMapping("/{cartId}")
    public ResponseEntity<CartDto> updateCart(
            @PathVariable Long cartId,
            @Valid @RequestBody CartUpdateDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();

        Cart updatedCart = cartService.updateCart(cartId, memberId, request.getQuantity());

        CartDto updatedCartDto = new CartDto(updatedCart);

        return ResponseEntity.ok(updatedCartDto);
    }

    /**
     * 장바구니 단일 삭제
     * @param cartId 삭제할 장바구니 ID
     * @param userDetails 인증된 사용자 정보
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(
            @PathVariable Long cartId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();

        cartService.deleteCart(cartId, memberId);

        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }

    /**
     * 장바구니 여러 항목 삭제 (예시)
     * @param cartIds 삭제할 장바구니 ID 리스트
     * @param userDetails 인증된 사용자 정보
     */
    @DeleteMapping("/batch-delete")
    public ResponseEntity<String> deleteCartList(
            @RequestBody List<Long> cartIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();

        cartService.deleteCartList(cartIds, memberId);

        return ResponseEntity.ok(ResponseMessageConstants.DELETE_SUCCESS);
    }
}





















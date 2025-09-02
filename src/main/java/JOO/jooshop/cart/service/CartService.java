package JOO.jooshop.cart.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartUpdateDto;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.Exception.ResponseMessageConstants.*;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepositoryV1 memberRepository;
    private final ProductManagementRepository productManagementRepository;

    /** =================== 장바구니 리스트 조회 =================== */
    public List<CartDto> allCarts(Long memberId) {
        // 사용자 검증
        verifyUserIdMatch(memberId);

        // DB에서 회원의 모든 Cart 엔티티 조회 후 DTO 변환
        return cartRepository.findByMemberId(memberId).stream()
                .map(CartDto::fromCart)
                .collect(Collectors.toList());
    }

    /** =================== 장바구니 담기 =================== */
    public Long addCart(Long memberId, Long inventoryId, int quantity) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        ProductManagement productMgt = productManagementRepository.findById(inventoryId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND + " inventoryId: " + inventoryId));

        // 사용자 검증
        verifyUserIdMatch(memberId);

        // 기존 장바구니 존재 여부 확인
        Cart existingCart = cartRepository.findByProductManagementAndMember(productMgt, member).orElse(null);

        if (existingCart != null) {
            // 기존 수량 덮어쓰기
            existingCart.setQuantity(quantity);
            existingCart.setPrice(productMgt.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(quantity)));
            return cartRepository.save(existingCart).getCartId();
        } else {
            // 새 Cart 생성
            BigDecimal price = productMgt.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));
            Cart cart = new Cart(member, productMgt, quantity, price);
            return cartRepository.save(cart).getCartId();
        }
    }

    /** =================== 장바구니 수량 수정 =================== */
    public CartDto updateCart(Long cartId, CartUpdateDto dto) {
        Long memberId = dto.getMemberId();
        verifyMember(cartId, memberId);

        Cart cart = findCartById(cartId);

        // 수량 변경
        cart.setQuantity(dto.getQuantity());
        // 단가 * 수량 = price 재계산
        cart.setPrice(cart.getProductManagement().getProduct().getPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity())));

        // 수정된 Cart 저장
        Cart updatedCart = cartRepository.save(cart);

        // DTO 변환 후 반환
        return CartDto.fromCart(updatedCart); // 또는 convertToDto(updatedCart)
    }

    /** =================== 장바구니 삭제 =================== */
    public void deleteCart(Long cartId, Long memberId) {
        verifyMember(cartId, memberId);
        cartRepository.delete(findCartById(cartId));
    }

    public void deleteCartList(List<Long> cartIds, Long memberId) {
        for (Long cartId : cartIds) {
            verifyMember(cartId, memberId);
            cartRepository.delete(findCartById(cartId));
        }
    }

    /** =================== 헬퍼 메서드 =================== */
    private Cart findCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND + " cartId: " + cartId));
    }

    private void verifyMember(Long cartId, Long memberId) {
        Cart cart = findCartById(cartId);
        if (memberId == null) throw new IllegalArgumentException("memberId가 null 입니다.");
        verifyUserIdMatch(memberId);
        if (!cart.getMember().getId().equals(memberId))
            throw new SecurityException("해당 장바구니에 대한 권한이 없습니다.");
    }
}

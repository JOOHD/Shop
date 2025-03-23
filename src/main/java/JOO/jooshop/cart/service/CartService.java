package JOO.jooshop.cart.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.ResponseMessageConstants.*;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepositoryV1 memberRepository;
    private ProductManagementRepository productManagementRepository;

    // Cart 조회 메서드 - 중복 코드 리팩토링
    private Cart findCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));
    }
    
    // Cart 조회 후 사용자 검증
    private void verifyMember(Long cartId, Long memberId) {
        Cart cart = findCartById(cartId);
        verifyUserIdMatch(cart.getMember().getId()); // 로그인안된 사용자와 요청 사용자 비교
    }

    /** =================== 공통 메서드 =================== */

    /**
     * 장바구니 담기
     * @param request
     * @param productMgtId
     * @return
     *
     * 사용자 정보와 상품 정보를 가져온다. 그 후 상품 + 사용자 정보로 장바구니에 이미 존재하는지 찾는다.
     * 동일 사용자가 (같은 상품/옵션) 선택 시, 수량과 가격을 올리고,
    (같은 상품, 다른 옵션) or (다른 상품) 선택 시, 따로 담는다.
     * 위에 상황을 위해, 상품 옵션 정보를 담은 ProductManagement table 생성
     *
     *  메서드 흐름
        1. DTO, 회원 정보와 수량 추출
        2. DB, 회원(Member) 조회
        3. DB, 상품 옵션(ProductManagement) 조회
        4. findByProductManagementAndMember() 메서드로 이미 담긴 상품인지 판별
        5. if문 진입 (이미 담긴 경우)
            → 수량, 가격 업데이트
            → 저장
        6.else문 진입 (새로운 경우)
            → Cart 엔티티 생성
            → 저장
     */
    public Long addCart(CartRequestDto request, Long productMgtId) {
        // addCart : 장바구니에 같은 옵션의 상품은 하나만 존재해야 한다.
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        ProductManagement productMgt = productManagementRepository.findById(productMgtId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND + " productMgtId: " + productMgtId));

        verifyUserIdMatch(request.getMemberId());

        // DB 조회를 통해, 이미 담긴 상품인지 판별, 기준은 엔티티 객체
        Cart existingCart = cartRepository.findByProductManagementAndMember(productMgt, member).orElse(null);

        // 장바구니에 동일 상품이 존재하면 수량·가격 수정, 없으면 새 상품 추가
        if (existingCart != null) { // 이미 담은 상품과 옵션인 경우 수량, 가격을 수정
            // 현재 수량 + 담은 수량
            existingCart.setQuantity(existingCart.getQuantity() + request.getQuantity());
            // 현재 가격 + 담은 가격
            existingCart.setPrice(existingCart.getPrice() + productMgt.getProduct().getPrice() * request.getQuantity());
            cartRepository.save(existingCart);
            return existingCart.getCartId();
        } else {
            Long price = productMgt.getProduct().getPrice() * request.getQuantity();
            Cart cart = new Cart(member, productMgt, request.getQuantity(), price);
            cartRepository.save(cart);
            return cart.getCartId();
        }
    }

    /**
     * 유저의 전체 장바구니 리스트 조회
     * @return
     */
    public List<CartDto> allCarts(Long memberId) {
        verifyUserIdMatch(memberId); // 로그인 된 사용자와 요청 사용자 비교

        List<Cart> carts = cartRepository.findByMemberId(memberId);
        return carts.stream()
                .map(CartDto::fromEntity) // cart -> new CartDto(cart)
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 수정 (상품 갯수 수정 -> 가격 변경)
     * @param cartId
     * @param updatedCart
     * @return
     */
    public Cart updateCart(Long cartId, Cart updatedCart) {
        verifyMember(cartId, updatedCart.getMember().getId()); // 사용자 검증

        Cart existingCart = findCartById(cartId);
        existingCart.setQuantity(updatedCart.getQuantity());
        Long price = existingCart.getProductManagement().getProduct().getPrice() * updatedCart.getQuantity();
        existingCart.setPrice(price);

        return cartRepository.save(existingCart);
    }

    /**
     * 장바구니 삭제
     * @param cartId
     */
    public void deleteCart(Long cartId) {
        verifyMember(cartId, null); // 사용자 검증

        Cart cart = findCartById(cartId);
        cartRepository.delete(cart);
    }

    /**
     * 여러 장바구니 한 번에 삭제
     * @param cartIds - 여러 cart 의 cartId 를 리스트로
     */
    public void deleteCartList(List<Long> cartIds) {
        System.out.println("Type of service cartIds: " + cartIds.getClass());

        for (Long cartId : cartIds) {

            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            cartRepository.delete(cart);
        }
    }
}



























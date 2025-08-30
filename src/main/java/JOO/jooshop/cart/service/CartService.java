package JOO.jooshop.cart.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.model.CartDto;
import JOO.jooshop.cart.model.CartRequestDto;
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

    /** =================== 공통 메서드 =================== */

    /**
     * 장바구니 담기
     * @param memberId 인증된 사용자의 ID (클라이언트에서 직접 받지 않고, 서버에서 추출된 값이어야 함)
     * @param inventoryId 상품 옵션 ID (상품관리 테이블 PK)
     * @param quantity 담을 수량
     * @return 생성 혹은 수정된 Cart 엔티티의 ID
     *
     * 사용자 정보와 상품 정보를 가져온다. 그 후 상품 + 사용자 정보로 장바구니에 이미 존재하는지 찾는다.
     * 동일 사용자가 (같은 상품/옵션) 선택 시, 수량과 가격을 올리고,
                  (같은 상품, 다른 옵션) or (다른 상품) 선택 시, 따로 담는다.
     * 위에 상황을 위해, 상품 옵션 정보를 담은 ProductManagement table 생성
     *
     *  - 인증된 memberId와 productManagementId, 수량(quantity)를 받아 처리
     *  - 같은 상품+옵션이 이미 장바구니에 있으면 수량과 가격을 합산하여 업데이트
     *  - 없으면 새로운 Cart 엔티티를 생성하여 저장
     *
     *  - ProductManagement = 옵션 조합 객체, inventoryId = 그 옵션 조합 객체의 고유 ID
     */
    public Long addCart(Long memberId, Long inventoryId, int quantity) {
        // 1. 회원 정보 조회 (DB에서 실제 존재 확인)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        // 2. 상품 옵션 조회 (상품 + 옵션이 유효한지 확인)
        ProductManagement productMgt = productManagementRepository.findById(inventoryId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND + " inventoryId: " + inventoryId));

        // 3. 사용자 인증 검증 (현재 로그인된 사용장와 요청 userId가 일치하는지 확인)
        verifyUserIdMatch(memberId);

        // 4. 이미 같은 상품 옵션을 장바구니에 담았는지 조회
        Cart existingCart = cartRepository.findByProductManagementAndMember(productMgt, member).orElse(null);

        if (existingCart != null) {
            // 5. 이미 담긴 상품이 있다면, 수량을 기존 값 + 새로 담는 수량만큼 증가
            existingCart.setQuantity(existingCart.getQuantity() + quantity);

            // 6. 가격도 기존 가격 + 새로 담는 상품 가격 * 수량 으로 업데이트
            BigDecimal priceToAdd = productMgt.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));
            existingCart.setPrice(existingCart.getPrice().add(priceToAdd));

            // 7. 변경된 Cart 저장
            cartRepository.save(existingCart);

            // 8. 기존 Cart 엔티티 ID 반환
            return existingCart.getCartId();
        } else {
            // 9. 같은 상품 옵션이 없으면 새로운 Cart 엔티티 생성
            BigDecimal price = productMgt.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));
            Cart cart = new Cart(member, productMgt, quantity, price);

            // 10. 새 Cart 저장
            cartRepository.save(cart);

            // 11. 새로 생성된 Cart 엔티티 ID 반환
            return cart.getCartId();
        }
    }

    /**
     * 사용자의 전체 장바구니 리스트 조회
     *
     * @param memberId 인증된 사용자 ID
     * @return 해당 사용자의 장바구니 상품 목록을 DTO 리스트로 반환
     */
    public List<CartDto> allCarts(Long memberId) {
        // 1. 인증된 사용자와 요청자 일치 여부 검증
        verifyUserIdMatch(memberId);

        // 2. DB에서 회원의 모든 Cart 엔티티 조회
        List<Cart> carts = cartRepository.findByMemberId(memberId);

        // 3. 조회된 엔티티 리스트를 DTO 리스트로 변환 후 반환
        return carts.stream()
                .map(CartDto::new) // 생성자 참조
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 상품 수량 및 가격 수정
     *
     * @param cartId 수정할 장바구니 항목 ID
     * @param memberId 인증된 사용자 ID
     * @param quantity 변경할 수량
     * @return 수정된 Cart 엔티티 반환
     */
    public Cart updateCart(Long cartId, Long memberId, int quantity) {
        // 1. 사용자 검증 (cartId에 연결된 회원과 요청한 memberId가 일치하는지)
        verifyMember(cartId, memberId);

        // 2. 수정 대상 Cart 엔티티 조회
        Cart existingCart = findCartById(cartId);

        // 3. 수량 수정
        existingCart.setQuantity(quantity);

        // 4. 수량 변경에 따른 가격 재계산 (상품 가격 * 수량)
        BigDecimal price = existingCart.getProductManagement().getProduct().getPrice()
                .multiply(BigDecimal.valueOf(quantity));
        existingCart.setPrice(price);

        // 5. 변경된 Cart 엔티티 저장 후 반환
        return cartRepository.save(existingCart);
    }

    /**
     * 장바구니 단일 삭제
     *
     * @param cartId 삭제할 장바구니 항목 ID
     * @param memberId 인증된 사용자 ID
     */
    public void deleteCart(Long cartId, Long memberId) {
        // 1. 사용자 검증
        verifyMember(cartId, memberId);

        // 2. 삭제 대상 Cart 조회
        Cart cart = findCartById(cartId);

        // 3. 삭제 수행
        cartRepository.delete(cart);
    }

    /**
     * 장바구니 여러 항목 한꺼번에 삭제
     *
     * @param cartIds 삭제할 Cart ID 리스트
     * @param memberId 인증된 사용자 ID
     */
    public void deleteCartList(List<Long> cartIds, Long memberId) {
        for (Long cartId : cartIds) {
            // 각 항목마다 사용자 검증 후 삭제 수행
            verifyMember(cartId, memberId);

            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. cartId: " + cartId));

            cartRepository.delete(cart);
        }
    }

    /**
     * 특정 Cart 엔티티 조회, 없으면 예외 발생
     *
     * @param cartId 장바구니 항목 ID
     * @return Cart 엔티티
     */
    private Cart findCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND + " cartId: " + cartId));
    }

    /**
     * CartId와 memberId가 일치하는지 검증
     * @param cartId 장바구니 ID
     * @param memberId 인증된 사용자 ID (null일 수 있으므로 필요시 인증된 ID 가져오기)
     */
    private void verifyMember(Long cartId, Long memberId) {
        Cart cart = findCartById(cartId);

        // 만약 memberId가 null이면, 인증된 사용자 ID를 내부에서 직접 가져오는 로직 필요 (예: SecurityContext)
        if (memberId == null) {
            // TODO: 인증된 사용자 ID 가져오기 (SecurityContextHolder 등)
            throw new IllegalArgumentException("memberId가 null 입니다. 인증된 사용자 ID를 전달해주세요.");
        }

        // 로그인된 사용자와 요청한 사용자가 같은지 검증
        verifyUserIdMatch(memberId);

        // Cart 엔티티의 회원 ID와 요청 회원 ID가 일치하는지 확인
        if (!cart.getMember().getId().equals(memberId)) {
            throw new SecurityException("해당 장바구니에 대한 권한이 없습니다.");
        }
    }
}
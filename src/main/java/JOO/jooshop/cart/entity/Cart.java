package JOO.jooshop.cart.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.productManagement.entity.ProductManagement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cart")
public class Cart {
    /*
        1. Cart를 aggregate root로 둠
        2. setter 제거
        3. 생성자는 protected
        4. 정적 팩토리 메서드 사용
        5. 수량 변경 시 내부에서 price 재계산
        6.member/productManagement/quantity/price validation 포함
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_mgt_id", nullable = false)
    private ProductManagement productManagement;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Cart 생성 시 불변식 검증 및 가격 자동 계산
     */
    private Cart(Member member, ProductManagement productManagement, int quantity) {
        validateCreate(member, productManagement, quantity);

        this.member = member;
        this.productManagement = productManagement;
        this.quantity = quantity;
        this.price = calculatePrice(productManagement, quantity);
    }

    /**
     * 장바구니 생성 팩토리 메서드 (외부에서 생성자 직접 호출 방지)
     */
    public static Cart createCart(Member member, ProductManagement productManagement, int quantity) {
        return new Cart(member, productManagement, quantity);
    }

    /**
     * 장바구니 수량 변경 + 가격 재계산
     */
    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
        this.price = calculatePrice(this.productManagement, quantity);
    }

    /**
     * 기존 장바구니 존재 시 수량 덮어쓰기
     */
    public void replaceQuantity(int quantity) {
        changeQuantity(quantity);
    }

    /**
     * 현재 로그인 사용자와 장바구니 소유자 일치 여부 확인
     */
    public boolean isOwnedBy(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return Objects.equals(this.member.getId(), memberId);
    }

    /**
     * 장바구니 소유자 ID 반환 (권한 검증용)
     */
    public Long getOwnerId() {
        return this.member.getId();
    }

    /**
     * 생성 시 필수 값 검증
     */
    private static void validateCreate(Member member, ProductManagement productManagement, int quantity) {
        if (member == null) {
            throw new IllegalArgumentException("회원은 null일 수 없습니다.");
        }
        if (productManagement == null) {
            throw new IllegalArgumentException("상품 옵션은 null일 수 없습니다.");
        }
        validateQuantity(quantity);
        validateUnitPrice(productManagement);
    }

    /**
     * 수량 유효성 검증 (1개 이상)
     */
    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }

    /**
     * 상품 가격 존재 여부 검증
     */
    private static void validateUnitPrice(ProductManagement productManagement) {
        Product product = productManagement.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 존재하지 않습니다.");
        }
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("상품 가격이 존재하지 않습니다.");
        }
    }

    /**
     * 단가 * 수량으로 총 가격 계산
     */
    private static BigDecimal calculatePrice(ProductManagement productManagement, int quantity) {
        return productManagement.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(quantity));
    }
}
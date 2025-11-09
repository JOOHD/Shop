package JOO.jooshop.cart.model;

import JOO.jooshop.cart.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartDto {
    private Long id;                     // 장바구니 ID
    private Long memberId;               // 로그인 사용자 ID
    private String memberName;           // 주문자명   
    private String phoneNumber;          // 전화번호
    private String productName;          // 상품명
    private String size;                 // 옵션/사이즈
    private double originalPrice;        // 원가 (단가)
    private int discountRate;            // 할인율 (%)
    private double finalPrice;           // 할인 적용된 단가
    private int quantity;                // 수량
    private double totalPrice;           // (할인 적용 단가 * 수량)
    private String productThumbnailUrl;  // 썸네일

    /** Entity -> DTO 변환 + 계산 수행 */
    public static CartDto toDto(Cart cart) {

        int quantity = cart.getQuantity();

        BigDecimal unitPrice = cart.getProductManagement().getProduct().getPrice();

        // DB에 저장된 할인율 (%), null 이면 0
        Integer discountRate = cart.getProductManagement().getProduct().getDiscountRate();
        int appliedRate = (discountRate != null) ? discountRate : 0;

        // 할인율 적용
        BigDecimal discountMultiplier = BigDecimal.valueOf(100 - appliedRate)
                .divide(BigDecimal.valueOf(100));

        // 할인 적용도니 단가
        BigDecimal finalPrice = unitPrice.multiply(discountMultiplier);

        // 총액
        BigDecimal totalPrice = finalPrice.multiply(BigDecimal.valueOf(quantity));

        // 썸네일 URL
        String thumbnailUrl = cart.getProductManagement().getProduct().getProductThumbnails().isEmpty()
                ? null
                : cart.getProductManagement().getProduct().getProductThumbnails().get(0).getImagePath();

        return CartDto.builder()
                .id(cart.getCartId())
                .productName(cart.getProductManagement().getProduct().getProductName())
                .size(cart.getProductManagement().getSize().toString())
                .originalPrice(unitPrice.doubleValue())          // 원가
                .discountRate(appliedRate)                       // 할인율
                .finalPrice(finalPrice.doubleValue())            // 할인 적용된 단가
                .quantity(quantity)                              // 수량
                .totalPrice(totalPrice.doubleValue())            // 총액
                .productThumbnailUrl(thumbnailUrl)
                .memberId(cart.getMember().getId())
                .memberName(cart.getMember().getUsername())      // 주문자명
                .phoneNumber(cart.getMember().getPhoneNumber())              // 전화번호
                .build();
    }
}

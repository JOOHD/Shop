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
    private String productName;          // 상품명
    private String size;                 // 옵션/사이즈
    private double price;                // 단가 * 수량
    private double discountAmount;       // 할인 금액
    private double finalPrice;           // price - discountAmount
    private int quantity;                // 수량
    private String productThumbnailUrl;  // 썸네일

    /** Cart 엔티티에서 DTO 변환 + 계산 수행 */
    public static CartDto fromCart(Cart cart) {
        BigDecimal unitPrice = cart.getProductManagement().getProduct().getPrice();
        BigDecimal discountRate = cart.getProductManagement().getProduct().getDiscountRate() != null
                ? BigDecimal.valueOf(cart.getProductManagement().getProduct().getDiscountRate())
                : BigDecimal.ZERO;
        int quantity = cart.getQuantity();

        // ---------------- 계산 로직 ----------------
        // 단가에서 할인 적용 후 수량 곱
        BigDecimal finalPriceBD = unitPrice.multiply(BigDecimal.ONE.subtract(discountRate))
                .multiply(BigDecimal.valueOf(quantity));
        // 원가 * 수량
        BigDecimal priceBD = unitPrice.multiply(BigDecimal.valueOf(quantity));
        // 할인금액
        BigDecimal discountAmountBD = priceBD.subtract(finalPriceBD);

        // 썸네일 URL
        String thumbnailUrl = cart.getProductManagement().getProduct().getProductThumbnails().isEmpty()
                ? null
                : cart.getProductManagement().getProduct().getProductThumbnails().get(0).getImagePath();

        return CartDto.builder()
                .id(cart.getCartId())
                .productName(cart.getProductManagement().getProduct().getProductName())
                .size(cart.getProductManagement().getSize().toString())
                .quantity(quantity)
                .price(priceBD.doubleValue())
                .discountAmount(discountAmountBD.doubleValue())
                .finalPrice(finalPriceBD.doubleValue())
                .productThumbnailUrl(thumbnailUrl)
                .build();
    }
}

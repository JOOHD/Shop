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
    private Long id;                     // 장바구니 ID (cartId → id 로 변경)
    private Long productId;              // 상품 ID
    private String productName;          // 상품명
    private String color;                // 색상
    private String size;                 // 사이즈
    private BigDecimal price;            // 가격 (productPrice → price)
    private int quantity;                // 수량
    private String productThumbnailUrl;  // 상품 썸네일 URL

    public CartDto(Cart cart) {
        this.id = cart.getCartId();
        this.productId = cart.getProductManagement().getProduct().getProductId();
        this.productName = cart.getProductManagement().getProduct().getProductName();
        this.color = cart.getProductManagement().getColor().getColor();
        this.size = cart.getProductManagement().getSize().toString();
        this.price = cart.getProductManagement().getProduct().getPrice();
        this.quantity = cart.getQuantity();
        this.productThumbnailUrl = cart.getProductManagement().getProduct()
                .getProductThumbnails().isEmpty() ? null :
                cart.getProductManagement().getProduct().getProductThumbnails().get(0).getImagePath();
    }
}

package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminProductResponseDto  {
    private Long productId;
    private String productName;
    private ProductType productType;
    private BigDecimal price;
    private String productInfo; // description 제거
    private String thumbnailUrl; // 목록용 thumbnail 추가
}
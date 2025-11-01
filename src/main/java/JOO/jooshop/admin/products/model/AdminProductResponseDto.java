package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminProductResponseDto {

    // 화면 출력용 확장 DTO

    private Long productId;
    private String productName;       // 화면용도 그대로
    private ProductType productType;
    private BigDecimal price;
    private String productInfo;
    private String thumbnailUrl;      // 목록용 썸네일 추가
    private LocalDateTime createdAt;  // 목록용 등록일 추가
}

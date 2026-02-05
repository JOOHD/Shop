package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin 상품 목록/상세 응답 DTO
 *
 * 정책:
 * - thumbnailUrl은 "클라이언트 렌더링용 URL"로 내려간다.
 *   (외부 URL이면 그대로, 로컬 상대경로면 /uploads/ prefix가 붙은 상태)
 * - 썸네일이 없으면 null 허용
 */
public record AdminProductResponseDto(
        Long productId,
        String productName,
        ProductType productType,
        BigDecimal price,
        Integer discountRate,
        String productInfo,
        String thumbnailUrl,
        LocalDateTime createdAt
) {}

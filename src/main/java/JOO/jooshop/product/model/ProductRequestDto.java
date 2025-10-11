package JOO.jooshop.product.model;

import JOO.jooshop.global.validation.ValidDiscountRate;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ValidDiscountRate // 커스텀 유효성 검사 애노테이션 적용
public class ProductRequestDto {

    /**
     * 상품 등록/수정 후, 요청 DTO, 조회용 DTO
     */

    @NotBlank(message = "상품 이름은 필수입니다.")
    private String productName;
    private ProductType productType;
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount = false;
    @Max(value = 100, message = "할인율은 100을 초과할 수 없습니다.")
    private Integer discountRate = null;
    private Boolean isRecommend = false;
}
